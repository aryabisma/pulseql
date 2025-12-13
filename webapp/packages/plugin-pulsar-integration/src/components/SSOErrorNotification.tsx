/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */

import { observer } from 'mobx-react-lite';
import { useService } from '@cloudbeaver/core-di';
import { PulsarSSOService } from '../PulsarSSOService.js';

interface SSOErrorNotificationProps {
  onDismiss?: () => void;
}

/**
 * Error notification component for SSO authentication failures
 */
export const SSOErrorNotification = observer<SSOErrorNotificationProps>(
  function SSOErrorNotification({ onDismiss }) {
    const ssoService = useService(PulsarSSOService);

    if (!ssoService.authError) {
      return null;
    }

    return (
      <div className="sso-error-notification" role="alert">
        <div className="error-icon">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
            <path d="M10 0C4.5 0 0 4.5 0 10s4.5 10 10 10 10-4.5 10-10S15.5 0 10 0zm1 15H9v-2h2v2zm0-4H9V5h2v6z"/>
          </svg>
        </div>
        <div className="error-message">
          <strong>Authentication Error</strong>
          <p>{ssoService.authError}</p>
        </div>
        {onDismiss && (
          <button className="dismiss-button" onClick={onDismiss} aria-label="Dismiss">
            ×
          </button>
        )}
      </div>
    );
  }
);
