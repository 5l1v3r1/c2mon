/*
 * $Id $
 * 
 * $Date$ $Revision$ $Author$
 * 
 * Copyright CERN ${year}, All Rights Reserved.
 */
package cern.c2mon.notification.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cern.c2mon.client.common.listener.DataTagUpdateListener;
import cern.c2mon.client.common.tag.ClientDataTagValue;
import cern.c2mon.client.core.C2monServiceGateway;
import cern.c2mon.client.core.manager.TagManager;
import cern.c2mon.notification.Notifier;
import cern.c2mon.notification.SubscriptionRegistry;
import cern.c2mon.notification.Tag;
import cern.c2mon.notification.TagCacheUpdateListener;
import cern.c2mon.notification.shared.ServiceException;
import cern.c2mon.notification.shared.Subscriber;
import cern.c2mon.notification.shared.Subscription;
import cern.c2mon.notification.shared.TagNotFoundException;
import cern.dmn2.core.Status;
import cern.dmn2.db.DiamonDbGateway;
import cern.dmn2.db.EntityData.Type;
import cern.dmn2.db.MetricData;


/**
 * 
 * 
 * @author ${user} 
 * @version $Revision$, $Date$, $Author$
 */
public class TagCache implements DataTagUpdateListener {

    private Logger logger = LoggerFactory.getLogger(TagCache.class);

    /**
     * Our internal list of tags.
     */
    protected final ConcurrentHashMap<Long, Tag> cache = new ConcurrentHashMap<Long, Tag>();

    private String localCacheFileName = "tag.cache";
    
    @Autowired
    private SubscriptionRegistry registry;
    
    private Notifier notifier;
    
    /**
     * our list of listeners which are interested in the tag updates
     */
    protected final List<TagCacheUpdateListener> listeners = new ArrayList<TagCacheUpdateListener>();
    
    
    /**
     * Constructor
     */
    public TagCache() {
        
    }
    
    /**
     * 
     * @param listener A {@link TagCacheUpdateListener}
     */
    public void registerListener(TagCacheUpdateListener listener) {
        listeners.add(listener);
    }
    
    /**
     * 
     * @param id the tag id to resolve 
     * @return the full resolved {@link Tag} with all its children.
     * @throws TagNotFoundException in case the id is not registered in C2Mon
     */
    public Tag resolveSubTags(Long id) throws TagNotFoundException {
        Tag root = get(id);
        
        if (root == null) {
            logger.debug("Resolving sub-tags for {}", id);
            HashMap<Long, Tag> list = new HashMap<Long, Tag>();
            root = metricTagResolver2(id, list);
            if (root == null) {
                logger.error("Tag {} cannot be resolved!", id);
            } else {
                for (Entry<Long, Tag> e : list.entrySet()) {
                    cache.put(e.getKey(), e.getValue());
                }
                cache.put(root.getId(), root);
            }
        }
        return root;
    }
    
    
    /** for each item in the passed list it resolves the sub tags and adds them all to it's internal list. 
     * 
     * @param registeredSubscriptionIds a list of tag ids (rules) which users are subscribed to. 
     * @return 
     */
    public void resolveSubTags(HashSet<Long> registeredSubscriptionIds) {
        logger.info("Resolving {} tags." , Integer.valueOf(registeredSubscriptionIds.size()));
        long t1 = System.currentTimeMillis();
        
        ExecutorService pool = Executors.newFixedThreadPool(4);
        final CountDownLatch latch = new CountDownLatch(registeredSubscriptionIds.size());
        for (final Long id : registeredSubscriptionIds) {
            pool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        resolveSubTags(id);
                        latch.countDown();
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.warn("Cannot resolve sub Tags: {}", e.getMessage());
                    }
                }
                
            });
        }
        pool.shutdown();
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        logger.info("Loaded {} related tag ids in {}msec" , Long.valueOf(getSize()), Long.valueOf((System.currentTimeMillis() - t1)));
    }

    /**
     * @param tagID a {@link Tag} object. No information is used from this object 
     *              except the {@link Tag#getId()}. 
     * @param overallList if not null all Tag objects will be put into the passed object
     * @return The 
     */
    @SuppressWarnings("unused")
    private Tag metricTagResolver(Long tagID, HashMap<Long, Tag> overallList) {
        
        if (get(tagID) != null) {
            logger.debug("Tag #{} already in cache. Returning current (local) object.", tagID);
            return get(tagID);
        }
        
        if (logger.isTraceEnabled()) {
            logger.trace("Tag #{}: Resolving the subtags using C2MonServer..", tagID);
        }
        
        checkExists(tagID);
        
        List<MetricData> list = DiamonDbGateway.getDbService().getEntityMetrics(Type.COMPUTER, tagID);
        
        Tag root = new Tag(tagID, true);

        for (MetricData md : list) {
            Tag childTag = null;
            
            if (md.hasLimit()) {
                Tag childRuleTag = new Tag(md.getLimitId(), true);
                childTag = new Tag(md.getId(), false);
                logger.trace("Adding Metric {} to {} ", childTag.getId(), tagID);
                childRuleTag.addChildTag(childTag);
                
                logger.trace("Adding Rule {} to {}" , md.getLimitId(), tagID);
                root.addChildTag(childRuleTag);

                if (overallList != null) {
                    overallList.put(childRuleTag.getId(), childRuleTag);
                    overallList.put(childTag.getId(), childTag);
                }
            } else {
                childTag = new Tag(md.getId(), false);
                logger.trace("Adding Metric {} to ", childTag.getId(), tagID);
                root.addChildTag(childTag);
                //children.put(childTag.getId(), childTag);
                if (overallList != null) {
                    overallList.put(childTag.getId(), childTag);
                }
            }
        }
        return root;
    }

    /**
     * 
     * @param tagId the id of the tag to resolve
     * @param overallList all found tagids related to the passed id will be added into this list
     * @return a {@link Tag} with all its children attached
     * @throws TagNotFoundException in case the passed id does not exist on the server.
     */
    Tag metricTagResolver2(final Long tagId, Map<Long, Tag> overallList) throws TagNotFoundException {
        
        if (get(tagId) != null) {
            logger.debug("Tag #{} already in cache. returning current (local) object.", tagId);
            return get(tagId);
        }
        
        if (logger.isTraceEnabled()) {
            logger.trace("Tag #{}: Resolving the subtags using C2MonServer..", tagId);
        }
        
        ClientDataTagValue parent = checkExists(tagId);
        
        Tag root = null;
        
        if (parent.isRuleResult()) {
            root = new Tag(tagId, true);
            if (overallList != null) {
                for (Long l : parent.getRuleExpression().getInputTagIds()) {
                    Tag child = metricTagResolver2(l, overallList);
                    root.addChildTag(child);
                    child.addParentTag(root);
                    overallList.put(child.getId(), child);
                    logger.trace("Got resolved child {} ", child);
                }
                overallList.put(tagId, root);
            } else {
                for (Long l : parent.getRuleExpression().getInputTagIds()) {
                    Tag child = metricTagResolver2(l, overallList);
                    root.addChildTag(child);
                    child.addParentTag(root);
                    logger.trace("Got resolved child {} ", child);
                }
            }
        } else {
            root = new Tag(tagId, false);
        }
        //root.update(parent);
        return root;
    }
    
   /**
    * 
    * @param tagId the tag to check
    * @return a {@link ClientDataTagValue} from the server
    * @throws TagNotFoundException in case the Tag is not either : not defined on server <code>ClientDataTagValue.getDataTagQuality().isExistingTag()</code>
    */
   private ClientDataTagValue checkExists(Long tagId) throws TagNotFoundException {
       HashSet<Long> toResolve = new HashSet<Long>();
       toResolve.add(tagId);
       Collection<ClientDataTagValue> oneElementList = C2monServiceGateway.getTagManager().getDataTags(toResolve);
      
       if (oneElementList.size() == 0) {
           throw new TagNotFoundException("Tag " + tagId + " not found.");
       }
       ClientDataTagValue parent = oneElementList.iterator().next();
       if (!parent.getDataTagQuality().isExistingTag()) {
           throw new TagNotFoundException("Tag " + tagId + " is not defined in server!");
       }
       return parent;
   }
    
    
    /**
     * 
     * @param id the id of the {@link Tag}
     * @return a {@link Tag} associated with the id or <code>NULL</code> if it does not exist.
     */
    public Tag get(Long id) {
        return cache.get(id);
    }

    /**
     * 
     * @return a Set of ids which are registered in this object
     */
    public Set<Long> getTagIds() {
        return cache.keySet();
    }
    
    /**
     * 
     * @return a HashMap of Tags which are declared as metrics. 
     */
    public HashMap<Long, Tag> getMetricTags() {
        HashMap<Long, Tag> result = new HashMap<Long, Tag>();
        for (Entry<Long, Tag> e : cache.entrySet()) {
            if (!e.getValue().isRule()) {
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }

    
    /**
     * Removes a subscription from all tags which are related. this method 
     * @param sub a {@link Subscription} object. It will be removed from all related sub-tags from the associated Tag.   
     */
    public void removeSubscription(Subscription sub) {
        Tag root = get(sub.getTagId());
        for (Tag child : root.getChildTags()) {
            child.removeSubscription(sub);
            if (child.getSubscribers().size() == 0) {
                cache.remove(child.getId());
            }
        }
    }
    

    public long getSize() {
        return cache.size();
    }
    
    /**
     * @return a String representation of this Cache.
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (Entry<Long, Tag> e : cache.entrySet()) {
            b.append("ID " + e.getKey() + " -> " + e.getValue().toString() + "\n");
        }
        return b.toString();
    }

    /**
     * checks :
     * <ul>
     * <li>if the passed update is existing : if not {@link SubscriptionRegistry#removeSubscription(Subscription)} for each found Subscription is called</li>
     * <li>if the passed update has an heartbeat expiration a warning is issued. Not further action</li>
     * </ul> 
     * @param tagUpdate the update
     */
    private void checkUpdateOk(ClientDataTagValue tagUpdate) {
        
        
        
        // we cannot do this, as we may have a remove/add operation for the same equipment
//        if (!tagUpdate.getDataTagQuality().isExistingTag()) {
//            for (Subscription s : registry.getSubscriptionsForTagId(tagUpdate.getId()).values()) {
//                registry.removeSubscription(s);
//            }
//            throw new IllegalStateException("TagID=" + tagUpdate.getId() + ": Is not known by the C2Mon server!");
//        }
        
//        if (tagUpdate.getDataTagQuality().isInvalidStatusSet(TagQualityStatus.SERVER_HEARTBEAT_EXPIRED)
//                || tagUpdate.getDataTagQuality().isInvalidStatusSet(TagQualityStatus.PROCESS_DOWN)
//                || tagUpdate.getDataTagQuality().isInvalidStatusSet(TagQualityStatus.JMS_CONNECTION_DOWN)
//                || tagUpdate.getDataTagQuality().isInvalidStatusSet(TagQualityStatus.SUBEQUIPMENT_DOWN)
//        ) {
//            logger.warn("TagID={} Got {}..", tagUpdate.getId());
//        } else if (!tagUpdate.getDataTagQuality().isAccessible()) {
//            logger.warn("TagID={} is reported as not accessible.", tagUpdate.getId());
//        } 
    }
    
    /**
     * 
     * @return a list of all uninitialized tags. <br>
     *         That is, all {@link Tag} which report {@link Tag#getLatestUpdate()} == NULL 
     */
    public List<Long> getUnitializedTags() {
        ArrayList<Long> allInitialized = new ArrayList<Long>();
        for (Entry<Long, Tag> e : cache.entrySet()) {
            if (e.getValue().getLatestUpdate() == null) {
                allInitialized.add(e.getKey());
            }
        }
        return allInitialized;
    }
    
    /**
     * 
     * @return an empty Listener for the initial updates. This is an empty implementation.
     */
    
    private class FirstUpdateListener implements DataTagUpdateListener {
        final CountDownLatch c;
        public FirstUpdateListener(HashSet<Long> tagIdListToExpect) {
            c = new CountDownLatch(tagIdListToExpect.size());
        }
        @Override
        public void onUpdate(ClientDataTagValue tagUpdate) {
            c.countDown();
            Tag tag = cache.get(tagUpdate.getId());
            if (tag == null) {
                throw new IllegalStateException("Cannot find Tag " + tagUpdate.getId() + " in my cache but got it from the server as an update.");
            }
            tag.update(tagUpdate);
        }
        public void waitUntilDone() throws InterruptedException {
            c.await(15, TimeUnit.SECONDS);
        }
        public long getLeft() {
            return c.getCount();
        }
    }
    
    
    @Override
    public void onUpdate(ClientDataTagValue tagUpdate) {
        
        try {
            
            logger.debug("TagID={} Update incoming: Name={}, isRule={}, value={}, valid={}, isAccessible={}, isExisting={}, equipIds={}, processIds={}", 
                    tagUpdate.getId(), tagUpdate.getName(), tagUpdate.isRuleResult(), tagUpdate.getValue(), 
                    tagUpdate.getDataTagQuality().isValid(), tagUpdate.getDataTagQuality().isAccessible(), 
                    tagUpdate.getDataTagQuality().isExistingTag(), tagUpdate.getEquipmentIds(), tagUpdate.getProcessIds());
            
            checkUpdateOk(tagUpdate);
            Tag tag = cache.get(tagUpdate.getId());
            
            if (tag == null) {
                logger.warn("TagID={} I have recevied an update I am not (yet) aware of. Discarding...", tagUpdate.getId());
                // TODO UNSUBSCRIBE from this item ?
                return;
            }
            
            if (tag.getSubscribers().size() == 0) {
                logger.warn("TagID={} There are no subscribers associated with this. Removing the C2Mon subscription.", tagUpdate.getId());
                cancelSubscriptionFor(tag.getId());
                return;
            }
            
            tag.update(tagUpdate);
            
            /*
             * triggers the notifier to notify for this tag
             */
            for (TagCacheUpdateListener l : listeners) {
                l.onUpdate(tag);
            }
            
        } catch (Exception ex) {
            logger.error("Error while getting an update from the server: ", ex);
        }
    }
  

    
    public void startSubscription(Subscription sub) {
        logger.info("Starting subscription on Tag {}", sub.getTagId());
        
//        HashSet<Long> toSubscribeTo = new HashSet<Long>();
//        
//        // let the cache resolve the subtags and then we subscribe to them here afterwards.
//        addResolvedSubTags(sub);
//        
//        Tag root = get(sub.getTagId()); 
//        if (root == null) {
//            /* not existent in current Cache : we need to get the latest from the server */
//            toSubscribeTo.addAll(sub.getResolvedSubTagIds());
//            toSubscribeTo.add(sub.getTagId());
//            
//            /* update the elements in the cache */
//            Collection<ClientDataTagValue> freshFromServer = getLatestFromServer(toSubscribeTo);
//            for (ClientDataTagValue c : freshFromServer) {
//                checkUpdateOk(c);
//                Tag cacheElement = cache.get(c.getId());
//                cacheElement.update(c);
//                //sub.setLastStatusForResolvedTSubTag(c.getId(), cacheElement.getLatestStatus());
//            }
//        }
//        
//
//        logger.debug("Starting subscription to " + toSubscribeTo.size() + " tags on C2Mon server.");
//        startSubscriptionFor(toSubscribeTo);
        HashSet<Subscription> s = new HashSet<Subscription>();
        s.add(sub);
        startSubscription(s);
    }
    
    /**
     * Adds the passed Subscription object to all child rules of the root Tag with ID={@link Subscription#getTagId()}
     * @param sub the user Subscription.
     */
    public void addResolvedSubTags(Subscription sub) {
        Tag root = this.get(sub.getTagId());
        
        if (root == null) {
            // let the cache resolve the subtags and then we subscribe to them here afterwards.
            root = resolveSubTags(sub.getTagId());
        } 
        
        for (Tag child : root.getAllChildTagsRecursive()) {
            if (child.isRule()) {
                // only RuleTags know about their subscription
                logger.trace("Adding subscription from User={} to resolved TagID={}", sub.getSubscriberId(), child.getId());
                child.addSubscription(sub);
                
                if (!sub.getResolvedSubTagIds().contains(child.getId())) {
                    sub.addResolvedSubTag(child.getId());
                    sub.setLastStatusForResolvedTSubTag(child.getId(), Status.OK);
                }
            } else {
                // anything here ?
            }
        }
        logger.trace("Adding subscription from User={} to TagID={}", sub.getSubscriberId(), sub.getTagId());
        root.addSubscription(sub);
        registry.updateLastModificationTime();
    }
    
    
    /** Resolves all sub-metrics from each element in <code>s</code> and using {@link TagCache#metricTagResolver(Tag, HashMap)} 
     *   and calls {@link TagManager#subscribeDataTags(java.util.Set, DataTagUpdateListener)}
     * 
     * @param list the list of tags to subscribe to.
     * @throws TagNotFoundException in case one of the passed tagid cannot be subscribed to.
     */
    public void startSubscription(HashSet<Subscription> list) throws TagNotFoundException {
        try {
            logger.trace("Entering startSubscription()");
            HashSet<Long> toSubscribeTo = new HashSet<Long>();
            
            /* loop over passed subscriptions and collect the new tags to subscribe to */
            for (Subscription s : list) {
                if (s.getLastNotifiedStatus().equals(Status.UNKNOWN)) {
                    s.setLastNotifiedStatus(Status.OK);
                }
                
            
                Tag t = get(s.getTagId());
                
                if (t == null) {
                    // unknown to the cache so far. need to resolve and 
                    // get latest values for initial notification
                    t = resolveSubTags(s.getTagId());
                } else {
                    // nothing to subscribe to 
                }
                
                for (Tag c : t.getAllChildTagsRecursive()) {
                    if (c.isRule()) {
                        if (!s.getResolvedSubTagIds().contains(c.getId())) {
                            s.addResolvedSubTag(c.getId());
                            s.setLastStatusForResolvedTSubTag(c.getId(), Status.OK);
                            
                            logger.trace("User={} subscribes to TagID={}", s.getSubscriberId(), s.getTagId());
                        }
                    } else {
                        // datatag 
                    }
                    
                    toSubscribeTo.add(c.getId());
                    c.addSubscription(s);
                    
                }
                
                t.addSubscription(s);
                toSubscribeTo.add(t.getId());
            }
            
            
            DataTagUpdateListener toCancelLater = startSubscriptionWithoutNotification(toSubscribeTo);
            
            for (Subscription s : list) {
                notifier.sendInitialReport(get(s.getTagId()));
            }
            
            logger.debug("Starting C2MON subscription to {} tags", toSubscribeTo.size());
            startSubscriptionFor(toSubscribeTo);
            
            unsubscribeFirstUpdateListener(toCancelLater);
            
            logger.trace("Leaving startSubscription()");
        } catch (TagNotFoundException ex) {
            throw ex;  
        } catch (Exception ex) {
            throw new ServiceException("Cannot subscribe to datatag : " + ex.getMessage());
        }
    }
    
    /**
     * 
     * @param toSubscribeTo the tags to subscribe to 
     * @throws InterruptedException in case we are interrupted while waiting for all updates.
     */
    DataTagUpdateListener startSubscriptionWithoutNotification(HashSet<Long> toSubscribeTo) throws InterruptedException {
        FirstUpdateListener fu = new FirstUpdateListener(toSubscribeTo);
        C2monServiceGateway.getTagManager().subscribeDataTags(toSubscribeTo, fu);
        fu.waitUntilDone();
        if (fu.getLeft() > 0) {
            logger.error("Not all updates arrived while waiting for initial subscription. Lef =" + fu.getLeft());
        }
        return fu;
    }
    
    /**
     * Required to cancel. Together with {@link #startSubscriptionWithoutNotification(HashSet)} 
     * this is a workaround used by {@link #startSubscription(HashSet)} to overcome the defect of the C2MON client 
     * API to provide a first collection when calling subscribe.  
     * @param initialReportTagListener
     */
    void unsubscribeFirstUpdateListener(DataTagUpdateListener initialReportTagListener) {
        C2monServiceGateway.getTagManager().unsubscribeAllDataTags(initialReportTagListener);
    }
    
    /**
     * Triggers the start of reception of the updates for the elements in the given list.
     * @param toSubscribeTo the list of items to subscribe to.
     */
    void startSubscriptionFor(HashSet<Long> toSubscribeTo) {
        if (toSubscribeTo.size() > 0) {
            C2monServiceGateway.getTagManager().subscribeDataTags(toSubscribeTo, this);
        }
    }
    
    /**
     * @see #cancelSubscriptionFor(HashSet)
     * @param l the tag id to cancel
     */
    void cancelSubscriptionFor(Long l) {
        HashSet<Long> toRemove = new HashSet<Long>();
        toRemove.add(l);
        cancelSubscriptionFor(toRemove);
    }
    
    /**
     * Cancels the subscription on the server and removes the tags from the internal list.
     * @param toCancel the list of tag ids to remove.
     */
    void cancelSubscriptionFor(HashSet<Long> toCancel) {
        C2monServiceGateway.getTagManager().unsubscribeDataTags(toCancel, this);
        for (Long l : toCancel) {
            this.cache.remove(l);
        }
    }
    
    /**
     * 
     * @param idList the list of tags you are interested in.
     * @return A list of {@link ClientDataTagValue} from the server.
     */
    public Collection<ClientDataTagValue> getLatestFromServer(HashSet<Long> idList) {
        return C2monServiceGateway.getTagManager().getDataTags(idList);
    }
    
    
    /** Triggers the {@link TagCache#removeTag(Long)} and calls {@link C2monServiceGateway#getTagManager()#cancelSubscription(Subscription)}. 
     * 
     * @see {@link SubscriptionRegistryImpl#setSubscriber(Subscriber)}
     * @see {@link SubscriptionRegistryImpl#removeSubscription(Subscription)} 
     * @param subscription the Subscription to cancel 
     * @throws ServiceException in case of an error in the communication. 
     */
    public void cancelSubscription(final Subscription subscription) throws ServiceException {
        
        // TODO : move into SubscriptionRegistry
        logger.trace("Entering cancelSubscription() for User={} and TagID={}", subscription.getSubscriberId(), subscription.getTagId());
        
        try {
            if (subscription.getResolvedSubTagIds().size() == 0) {
                logger.info("The passed subscription User={}, Tag={} does not contain any resolved sub tags.", subscription.getSubscriberId(), subscription.getTagId());
            }
            
            Tag t = get(subscription.getTagId());
            HashSet<Long> toRemove = new HashSet<Long>();
            if (t != null) {
            
                /*
                 * remove the tags from the cache
                 */
                
                for (Tag ct : t.getChildTags()) {
                    ct.removeSubscription(subscription);
                    if (ct.getSubscribers().size() == 0) {
                        toRemove.add(ct.getId());
                    }
                }
            
                t.removeSubscription(subscription);
                if (t.getSubscribers().size() == 0) {
                    toRemove.add(t.getId());
                }
            } else {
                toRemove.add(subscription.getTagId());
            }
            
            /*
             * let's cancel the the subscription 
             */
            cancelSubscriptionFor(toRemove);
            
        } catch (Exception ex) {
            throw new ServiceException(ex);
        }
    }

    /**
     * @return Returns the registry.
     */
    public SubscriptionRegistry getRegistry() {
        return registry;
    }

    /**
     * @param registry The registry to set.
     */
    public void setRegistry(SubscriptionRegistry registry) {
        logger.trace("Setting registry.");
        this.registry = registry;
    }

    /**
     * @return Returns the fileName.
     */
    public String getLocalCacheFileName() {
        return localCacheFileName;
    }

    /**
     * @param fileName The fileName to set.
     */
    public void setLocalCacheFileName(String fileName) {
        logger.trace("Setting cache file to {}", fileName);
        this.localCacheFileName = fileName;
    }

    public void setNotifier(Notifier notifier) {
        this.notifier = notifier;
    }
    
    
}
