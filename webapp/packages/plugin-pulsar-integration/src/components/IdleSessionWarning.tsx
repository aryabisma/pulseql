/*
 * CloudBeaver - Pulsar Integration Plugin
 * Idle Session Warning Component
 */

import React, { useEffect, useState } from 'react';
import { observer } from 'mobx-react-lite';
import { useService } from '@cloudbeaver/core-di';
import { ActivityTrackingService } from '../ActivityTrackingService';

export const IdleSessionWarning: React.FC = observer(() => {
  const activityService = useService(ActivityTrackingService);
  const [showWarning, setShowWarning] = useState(false);

  useEffect(() => {
    // Register idle warning callback
    activityService.onIdleWarning(() => {
      setShowWarning(true);
    });

    // Register idle timeout callback
    activityService.onIdleTimeout(() => {
      // Session expired, redirect or show message
      alert('Your session has expired due to inactivity. Please log in again.');
      // Optionally redirect to login or Pulsar
      window.location.href = '/login';
    });
  }, [activityService]);

  const handleStayActive = () => {
    activityService.resetActivity();
    setShowWarning(false);
  };

  const handleLogout = () => {
    window.location.href = '/logout';
  };

  if (!showWarning) {
    return null;
  }

  return (
    <div className="idle-session-warning-overlay">
      <div className="idle-session-warning">
        <div className="warning-icon">
          <i className="icon-alert"></i>
        </div>
        
        <h2>Are you still there?</h2>
        
        <p>
          You've been inactive for a while. Your session will expire in:
        </p>
        
        <div className="countdown-timer">
          <span className="time-remaining">
            {activityService.formattedTimeUntilIdle}
          </span>
        </div>
        
        <div className="warning-actions">
          <button onClick={handleStayActive} className="btn-primary">
            Yes, I'm still here
          </button>
          <button onClick={handleLogout} className="btn-secondary">
            Log out
          </button>
        </div>
      </div>
    </div>
  );
});
