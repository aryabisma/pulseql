/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */

import { observer } from 'mobx-react-lite';
import { useService } from '@cloudbeaver/core-di';
import { PulsarUICustomizer } from '../PulsarUICustomizer.js';
import { WorkspaceModeService } from '../WorkspaceModeService.js';

/**
 * Button component to navigate back to Pulsar application
 */
export const BackToPulsarButton = observer(function BackToPulsarButton() {
  const uiCustomizer = useService(PulsarUICustomizer);

  if (!uiCustomizer.shouldShowBackToPulsarButton()) {
    return null;
  }

  const workspaceModeService = useService(WorkspaceModeService);
  const branding = workspaceModeService.getCustomBranding();

  return (
    <button
      className="back-to-pulsar-button"
      onClick={() => uiCustomizer.navigateBackToPulsar()}
      title="Return to Pulsar application"
    >
      <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
        <path d="M8 0L1 7l7 7V9c5 0 7 3 7 7 0-6-2-9-7-9V0z"/>
      </svg>
      <span>Back to {branding?.title || 'Pulsar'}</span>
    </button>
  );
});
