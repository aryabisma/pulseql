/*
 * CloudBeaver - Pulsar Integration Plugin
 * Activity Tracking Service
 * 
 * Tracks user activity for security and session management
 */

import { injectable } from '@cloudbeaver/core-di';
import { makeObservable, observable, action, computed } from 'mobx';

export interface ActivityEvent {
  type: 'query' | 'navigation' | 'export' | 'connection' | 'idle' | 'active';
  timestamp: number;
  details?: Record<string, any>;
}

export interface SessionActivity {
  sessionId: string;
  userId: string;
  startTime: number;
  lastActivityTime: number;
  events: ActivityEvent[];
  isActive: boolean;
}

const IDLE_TIMEOUT_MS = 15 * 60 * 1000; // 15 minutes
const WARNING_TIMEOUT_MS = 13 * 60 * 1000; // 13 minutes (2 min warning)
const ACTIVITY_CHECK_INTERVAL = 30 * 1000; // 30 seconds
const MAX_EVENTS_STORED = 50;

@injectable()
export class ActivityTrackingService {
  private sessionActivity: SessionActivity | null = null;
  private lastActivityTime: number = Date.now();
  private idleWarningShown: boolean = false;
  private activityCheckTimer: number | null = null;
  private idleWarningCallback: (() => void) | null = null;
  private idleTimeoutCallback: (() => void) | null = null;

  constructor() {
    makeObservable<this, 'sessionActivity' | 'lastActivityTime' | 'idleWarningShown'>(this, {
      sessionActivity: observable,
      lastActivityTime: observable,
      idleWarningShown: observable,
      trackActivity: action,
      resetActivity: action,
      isIdle: computed,
      timeSinceLastActivity: computed,
    });

    this.setupActivityTracking();
  }

  /**
   * Initialize session activity tracking
   */
  initializeSession(sessionId: string, userId: string): void {
    this.sessionActivity = {
      sessionId,
      userId,
      startTime: Date.now(),
      lastActivityTime: Date.now(),
      events: [],
      isActive: true,
    };
    this.lastActivityTime = Date.now();
    this.startActivityMonitoring();
  }

  /**
   * Track user activity
   */
  trackActivity(type: ActivityEvent['type'], details?: Record<string, any>): void {
    const now = Date.now();
    this.lastActivityTime = now;
    this.idleWarningShown = false;

    if (this.sessionActivity) {
      this.sessionActivity.lastActivityTime = now;
      this.sessionActivity.isActive = true;

      // Add event to history
      const event: ActivityEvent = {
        type,
        timestamp: now,
        details,
      };

      this.sessionActivity.events.push(event);

      // Limit stored events
      if (this.sessionActivity.events.length > MAX_EVENTS_STORED) {
        this.sessionActivity.events = this.sessionActivity.events.slice(-MAX_EVENTS_STORED);
      }

      // Send activity to backend for session validation
      this.sendActivityToBackend(event);
    }
  }

  /**
   * Check if user is idle
   */
  get isIdle(): boolean {
    return this.timeSinceLastActivity > IDLE_TIMEOUT_MS;
  }

  /**
   * Get time since last activity (in milliseconds)
   */
  get timeSinceLastActivity(): number {
    return Date.now() - this.lastActivityTime;
  }

  /**
   * Get time until idle timeout (in milliseconds)
   */
  get timeUntilIdleTimeout(): number {
    const remaining = IDLE_TIMEOUT_MS - this.timeSinceLastActivity;
    return Math.max(0, remaining);
  }

  /**
   * Get formatted time until idle
   */
  get formattedTimeUntilIdle(): string {
    const ms = this.timeUntilIdleTimeout;
    const minutes = Math.floor(ms / 60000);
    const seconds = Math.floor((ms % 60000) / 1000);
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }

  /**
   * Reset activity timer
   */
  resetActivity(): void {
    this.lastActivityTime = Date.now();
    this.idleWarningShown = false;
    this.trackActivity('active');
  }

  /**
   * Register idle warning callback
   */
  onIdleWarning(callback: () => void): void {
    this.idleWarningCallback = callback;
  }

  /**
   * Register idle timeout callback
   */
  onIdleTimeout(callback: () => void): void {
    this.idleTimeoutCallback = callback;
  }

  /**
   * Get session statistics
   */
  getSessionStats(): {
    duration: number;
    queryCount: number;
    navigationCount: number;
    exportCount: number;
    idleTime: number;
  } | null {
    if (!this.sessionActivity) {
      return null;
    }

    const duration = Date.now() - this.sessionActivity.startTime;
    const queryCount = this.sessionActivity.events.filter(e => e.type === 'query').length;
    const navigationCount = this.sessionActivity.events.filter(e => e.type === 'navigation').length;
    const exportCount = this.sessionActivity.events.filter(e => e.type === 'export').length;
    const idleTime = this.isIdle ? this.timeSinceLastActivity : 0;

    return {
      duration,
      queryCount,
      navigationCount,
      exportCount,
      idleTime,
    };
  }

  /**
   * Get recent activity
   */
  getRecentActivity(count: number = 10): ActivityEvent[] {
    if (!this.sessionActivity) {
      return [];
    }
    return this.sessionActivity.events.slice(-count);
  }

  /**
   * Export session activity for audit
   */
  exportSessionActivity(): SessionActivity | null {
    return this.sessionActivity;
  }

  /**
   * Setup activity tracking listeners
   */
  private setupActivityTracking(): void {
    // Track mouse movement
    window.addEventListener('mousemove', this.handleUserActivity);
    
    // Track keyboard input
    window.addEventListener('keydown', this.handleUserActivity);
    
    // Track clicks
    window.addEventListener('click', this.handleUserActivity);
    
    // Track scroll
    window.addEventListener('scroll', this.handleUserActivity);
  }

  /**
   * Handle user activity event
   */
  private handleUserActivity = (): void => {
    // Throttle activity tracking to avoid excessive events
    const now = Date.now();
    if (now - this.lastActivityTime > 5000) { // Only track every 5 seconds
      this.resetActivity();
    }
  };

  /**
   * Start activity monitoring
   */
  private startActivityMonitoring(): void {
    this.activityCheckTimer = window.setInterval(() => {
      this.checkIdleStatus();
    }, ACTIVITY_CHECK_INTERVAL);
  }

  /**
   * Check idle status
   */
  private checkIdleStatus(): void {
    const timeSinceActivity = this.timeSinceLastActivity;

    // Show warning before timeout
    if (timeSinceActivity > WARNING_TIMEOUT_MS && !this.idleWarningShown) {
      this.idleWarningShown = true;
      if (this.idleWarningCallback) {
        this.idleWarningCallback();
      }
      this.trackActivity('idle', { warning: true });
    }

    // Handle idle timeout
    if (timeSinceActivity > IDLE_TIMEOUT_MS) {
      if (this.sessionActivity) {
        this.sessionActivity.isActive = false;
      }
      if (this.idleTimeoutCallback) {
        this.idleTimeoutCallback();
      }
      this.trackActivity('idle', { timeout: true });
    }
  }

  /**
   * Send activity to backend
   */
  private async sendActivityToBackend(event: ActivityEvent): Promise<void> {
    try {
      // Send activity event to backend for session validation
      await fetch('/api/sso/activity', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          sessionId: this.sessionActivity?.sessionId,
          event,
        }),
      });
    } catch (error) {
      // Silently fail - activity tracking is not critical
      console.debug('Failed to send activity to backend:', error);
    }
  }

  /**
   * Cleanup
   */
  dispose(): void {
    if (this.activityCheckTimer) {
      clearInterval(this.activityCheckTimer);
    }
    
    window.removeEventListener('mousemove', this.handleUserActivity);
    window.removeEventListener('keydown', this.handleUserActivity);
    window.removeEventListener('click', this.handleUserActivity);
    window.removeEventListener('scroll', this.handleUserActivity);
  }
}
