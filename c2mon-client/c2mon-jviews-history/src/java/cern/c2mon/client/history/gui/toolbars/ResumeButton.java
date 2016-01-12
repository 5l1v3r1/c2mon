/******************************************************************************
 * Copyright (C) 2010-2016 CERN. All rights not expressly granted are reserved.
 * 
 * This file is part of the CERN Control and Monitoring Platform 'C2MON'.
 * C2MON is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the license.
 * 
 * C2MON is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with C2MON. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/
package cern.c2mon.client.history.gui.toolbars;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.c2mon.client.ext.history.C2monHistoryGateway;
import cern.c2mon.client.ext.history.common.event.PlaybackControlListener;
import cern.c2mon.client.ext.history.common.exception.HistoryPlayerNotActiveException;

/**
 * The play/pause button for resuming and pausing the history playback.
 * 
 * @see TimHistoryPlayerToolBar
 * 
 * @author vdeila
 * 
 */
public class ResumeButton extends StandardButton implements PlaybackControlListener {

  /** serialVersionUID */
  private static final long serialVersionUID = -1287455560920408796L;

  /** Log4j logger for this class */
  private static final Logger LOG = LoggerFactory.getLogger(ResumeButton.class);

  /** The icon for the play button */
  private static final ImageIcon PLAY_ICON = new ImageIcon(Toolkit.getDefaultToolkit().getImage(TimHistoryPlayerToolBar.class.getResource("play.gif")));

  /** The icon for the pause button */
  private static final ImageIcon PAUSE_ICON = new ImageIcon(Toolkit.getDefaultToolkit().getImage(TimHistoryPlayerToolBar.class.getResource("pause.gif")));
  
  /**
   * <code>true</code> if the {@link #playButton} is playing (ie. showing the
   * pause sign)
   */
  private boolean playButtonIsPlaying;

  /**
   * Create a resume / pause button
   */
  public ResumeButton() {
    super();
    
    this.setIcon(PLAY_ICON);
    this.playButtonIsPlaying = false;
    
    this.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (playButtonIsPlaying) {
          pause();
        }
        else {
          play();
        }
      }
    });
  }

  /**
   * This method is called when the user has pressed the pause button.
   */
  private void pause() {
    if (this.playButtonIsPlaying) {
      try {
        C2monHistoryGateway.getHistoryManager().getHistoryPlayer().getPlaybackControl().pause();
      }
      catch (HistoryPlayerNotActiveException e) {
        LOG.debug("Cannot pause", e);
      }
    }
  }

  /**
   * This method is called when the user has pressed the play button.
   */
  private void play() {
    if (!this.playButtonIsPlaying) {
      try {
        C2monHistoryGateway.getHistoryManager().getHistoryPlayer().getPlaybackControl().resume();
      }
      catch (HistoryPlayerNotActiveException e) {
        LOG.debug("Cannot resume", e);
      }
    }
  }

  //
  // Events for when the history player is paused, resumed, etc.
  //
  @Override
  public void onPlaybackStarting() {
    playButtonIsPlaying = true;
    setIcon(PAUSE_ICON);
  }

  @Override
  public void onPlaybackStopped() {
    playButtonIsPlaying = false;
    setIcon(PLAY_ICON);
  }

  @Override
  public void onClockPlaybackSpeedChanged(final double newMultiplier) { }
  @Override
  public void onClockTimeChanged(final long newTime) { }
  @Override
  public void onClockTimeChanging(final long newTime) { }
  
}
