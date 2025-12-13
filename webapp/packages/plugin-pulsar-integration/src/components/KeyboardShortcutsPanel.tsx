/*
 * CloudBeaver - Pulsar Integration Plugin
 * Keyboard Shortcuts Help Panel Component
 */

import React, { useState } from 'react';
import { observer } from 'mobx-react-lite';
import { useService } from '@cloudbeaver/core-di';
import { KeyboardShortcutsService, KeyboardShortcut } from '../KeyboardShortcutsService';

export const KeyboardShortcutsPanel: React.FC<{ onClose?: () => void }> = observer(({ onClose }) => {
  const shortcutsService = useService(KeyboardShortcutsService);
  const [selectedContext, setSelectedContext] = useState<string>('global');
  const [editingShortcut, setEditingShortcut] = useState<KeyboardShortcut | null>(null);

  const contexts = ['global', 'editor', 'results', 'navigation'];
  const shortcuts = shortcutsService.getShortcutsByContext(selectedContext);

  const handleToggleShortcut = (id: string) => {
    shortcutsService.toggleShortcut(id);
  };

  const handleResetShortcut = (id: string) => {
    if (confirm('Reset this shortcut to default?')) {
      shortcutsService.resetShortcut(id);
    }
  };

  const handleExport = () => {
    const config = shortcutsService.exportConfig();
    const blob = new Blob([JSON.stringify(config, null, 2)], {
      type: 'application/json',
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `keyboard-shortcuts-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="keyboard-shortcuts-panel">
      <div className="shortcuts-header">
        <h2>Keyboard Shortcuts</h2>
        {onClose && (
          <button onClick={onClose} className="btn-close">
            ×
          </button>
        )}
      </div>

      <div className="shortcuts-actions">
        <button onClick={handleExport} className="btn-secondary">
          Export Configuration
        </button>
        <div className="context-tabs">
          {contexts.map(context => (
            <button
              key={context}
              onClick={() => setSelectedContext(context)}
              className={`context-tab ${selectedContext === context ? 'active' : ''}`}
            >
              {context.charAt(0).toUpperCase() + context.slice(1)}
            </button>
          ))}
        </div>
      </div>

      <div className="shortcuts-list">
        <table className="shortcuts-table">
          <thead>
            <tr>
              <th>Action</th>
              <th>Shortcut</th>
              <th>Enabled</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {shortcuts.map(shortcut => (
              <tr key={shortcut.id} className={!shortcut.enabled ? 'disabled' : ''}>
                <td className="shortcut-description">
                  {shortcut.description}
                </td>
                <td className="shortcut-key">
                  <kbd>{shortcut.key}</kbd>
                </td>
                <td className="shortcut-enabled">
                  <input
                    type="checkbox"
                    checked={shortcut.enabled}
                    onChange={() => handleToggleShortcut(shortcut.id)}
                  />
                </td>
                <td className="shortcut-actions">
                  <button
                    onClick={() => handleResetShortcut(shortcut.id)}
                    className="btn-icon"
                    title="Reset to default"
                  >
                    <i className="icon-reset"></i>
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="shortcuts-footer">
        <p className="text-muted">
          Press <kbd>F1</kbd> to show this help panel
        </p>
        <p className="text-muted">
          Note: Some shortcuts may conflict with browser shortcuts
        </p>
      </div>
    </div>
  );
});
