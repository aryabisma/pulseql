/*
 * CloudBeaver - Pulsar Integration Plugin
 * Keyboard Shortcuts Service
 * 
 * Manages keyboard shortcuts for power users
 */

import { injectable } from '@cloudbeaver/core-di';
import { makeObservable, observable, action } from 'mobx';

export interface KeyboardShortcut {
  id: string;
  key: string; // e.g., "Ctrl+Enter", "Alt+S"
  description: string;
  action: () => void;
  context?: 'global' | 'editor' | 'results' | 'navigation';
  enabled: boolean;
}

export interface ShortcutConfig {
  id: string;
  key: string;
  enabled: boolean;
}

const SHORTCUTS_STORAGE_KEY = 'pulsar_keyboard_shortcuts';

@injectable()
export class KeyboardShortcutsService {
  private shortcuts: Map<string, KeyboardShortcut> = new Map();
  private customShortcuts: Map<string, string> = new Map(); // id -> custom key
  private enabled: boolean = true;

  constructor() {
    makeObservable<this, 'shortcuts' | 'enabled'>(this, {
      shortcuts: observable,
      enabled: observable,
      registerShortcut: action,
      unregisterShortcut: action,
      toggleEnabled: action,
    });

    this.loadCustomShortcuts();
    this.registerDefaultShortcuts();
    this.setupEventListener();
  }

  /**
   * Register default shortcuts
   */
  private registerDefaultShortcuts(): void {
    // SQL Editor shortcuts
    this.registerShortcut({
      id: 'execute-query',
      key: 'Ctrl+Enter',
      description: 'Execute current query',
      action: () => this.triggerAction('execute-query'),
      context: 'editor',
      enabled: true,
    });

    this.registerShortcut({
      id: 'execute-selection',
      key: 'Ctrl+Shift+Enter',
      description: 'Execute selected text',
      action: () => this.triggerAction('execute-selection'),
      context: 'editor',
      enabled: true,
    });

    this.registerShortcut({
      id: 'format-query',
      key: 'Ctrl+Shift+F',
      description: 'Format SQL query',
      action: () => this.triggerAction('format-query'),
      context: 'editor',
      enabled: true,
    });

    this.registerShortcut({
      id: 'comment-line',
      key: 'Ctrl+/',
      description: 'Toggle line comment',
      action: () => this.triggerAction('comment-line'),
      context: 'editor',
      enabled: true,
    });

    this.registerShortcut({
      id: 'save-query',
      key: 'Ctrl+S',
      description: 'Save query to favorites',
      action: () => this.triggerAction('save-query'),
      context: 'editor',
      enabled: true,
    });

    // Navigation shortcuts
    this.registerShortcut({
      id: 'focus-search',
      key: 'Ctrl+K',
      description: 'Focus search box',
      action: () => this.triggerAction('focus-search'),
      context: 'global',
      enabled: true,
    });

    this.registerShortcut({
      id: 'toggle-sidebar',
      key: 'Ctrl+B',
      description: 'Toggle sidebar',
      action: () => this.triggerAction('toggle-sidebar'),
      context: 'global',
      enabled: true,
    });

    this.registerShortcut({
      id: 'open-history',
      key: 'Ctrl+H',
      description: 'Open query history',
      action: () => this.triggerAction('open-history'),
      context: 'global',
      enabled: true,
    });

    this.registerShortcut({
      id: 'open-favorites',
      key: 'Ctrl+Shift+H',
      description: 'Open favorites',
      action: () => this.triggerAction('open-favorites'),
      context: 'global',
      enabled: true,
    });

    // Results shortcuts
    this.registerShortcut({
      id: 'export-results',
      key: 'Ctrl+E',
      description: 'Export query results',
      action: () => this.triggerAction('export-results'),
      context: 'results',
      enabled: true,
    });

    this.registerShortcut({
      id: 'copy-results',
      key: 'Ctrl+Shift+C',
      description: 'Copy results to clipboard',
      action: () => this.triggerAction('copy-results'),
      context: 'results',
      enabled: true,
    });

    this.registerShortcut({
      id: 'next-tab',
      key: 'Ctrl+Tab',
      description: 'Next result tab',
      action: () => this.triggerAction('next-tab'),
      context: 'results',
      enabled: true,
    });

    this.registerShortcut({
      id: 'prev-tab',
      key: 'Ctrl+Shift+Tab',
      description: 'Previous result tab',
      action: () => this.triggerAction('prev-tab'),
      context: 'results',
      enabled: true,
    });

    // General shortcuts
    this.registerShortcut({
      id: 'help',
      key: 'F1',
      description: 'Show keyboard shortcuts help',
      action: () => this.triggerAction('help'),
      context: 'global',
      enabled: true,
    });

    this.registerShortcut({
      id: 'back-to-pulsar',
      key: 'Escape',
      description: 'Return to Pulsar',
      action: () => this.triggerAction('back-to-pulsar'),
      context: 'global',
      enabled: true,
    });
  }

  /**
   * Register a keyboard shortcut
   */
  registerShortcut(shortcut: KeyboardShortcut): void {
    this.shortcuts.set(shortcut.id, shortcut);
  }

  /**
   * Unregister a keyboard shortcut
   */
  unregisterShortcut(id: string): void {
    this.shortcuts.delete(id);
  }

  /**
   * Get all shortcuts
   */
  getAllShortcuts(): KeyboardShortcut[] {
    return Array.from(this.shortcuts.values());
  }

  /**
   * Get shortcuts by context
   */
  getShortcutsByContext(context: string): KeyboardShortcut[] {
    return this.getAllShortcuts().filter(s => s.context === context || s.context === 'global');
  }

  /**
   * Update shortcut key
   */
  updateShortcutKey(id: string, newKey: string): void {
    const shortcut = this.shortcuts.get(id);
    if (shortcut) {
      shortcut.key = newKey;
      this.customShortcuts.set(id, newKey);
      this.saveCustomShortcuts();
    }
  }

  /**
   * Reset shortcut to default
   */
  resetShortcut(id: string): void {
    this.customShortcuts.delete(id);
    this.saveCustomShortcuts();
    // Re-register default shortcuts
    this.registerDefaultShortcuts();
  }

  /**
   * Enable/disable shortcuts globally
   */
  toggleEnabled(): void {
    this.enabled = !this.enabled;
  }

  /**
   * Enable/disable specific shortcut
   */
  toggleShortcut(id: string): void {
    const shortcut = this.shortcuts.get(id);
    if (shortcut) {
      shortcut.enabled = !shortcut.enabled;
    }
  }

  /**
   * Handle keyboard event
   */
  private handleKeyboardEvent = (event: KeyboardEvent): void => {
    if (!this.enabled) {
      return;
    }

    const key = this.getKeyString(event);
    
    for (const shortcut of this.shortcuts.values()) {
      if (shortcut.enabled && shortcut.key === key) {
        event.preventDefault();
        event.stopPropagation();
        shortcut.action();
        break;
      }
    }
  };

  /**
   * Convert keyboard event to key string
   */
  private getKeyString(event: KeyboardEvent): string {
    const parts: string[] = [];
    
    if (event.ctrlKey || event.metaKey) parts.push('Ctrl');
    if (event.altKey) parts.push('Alt');
    if (event.shiftKey) parts.push('Shift');
    
    // Add the main key
    const key = event.key === ' ' ? 'Space' : event.key;
    parts.push(key);
    
    return parts.join('+');
  }

  /**
   * Setup global event listener
   */
  private setupEventListener(): void {
    window.addEventListener('keydown', this.handleKeyboardEvent);
  }

  /**
   * Cleanup event listener
   */
  dispose(): void {
    window.removeEventListener('keydown', this.handleKeyboardEvent);
  }

  /**
   * Trigger action (to be handled by registered handlers)
   */
  private triggerAction(actionId: string): void {
    // Dispatch custom event for action handlers
    window.dispatchEvent(new CustomEvent('pulsar-shortcut-action', {
      detail: { actionId },
    }));
  }

  /**
   * Load custom shortcuts from storage
   */
  private loadCustomShortcuts(): void {
    try {
      const data = localStorage.getItem(SHORTCUTS_STORAGE_KEY);
      if (data) {
        const shortcuts = JSON.parse(data) as ShortcutConfig[];
        shortcuts.forEach(config => {
          this.customShortcuts.set(config.id, config.key);
        });
      }
    } catch (error) {
      console.error('Failed to load custom shortcuts:', error);
    }
  }

  /**
   * Save custom shortcuts to storage
   */
  private saveCustomShortcuts(): void {
    try {
      const configs: ShortcutConfig[] = Array.from(this.customShortcuts.entries()).map(
        ([id, key]) => ({
          id,
          key,
          enabled: this.shortcuts.get(id)?.enabled ?? true,
        })
      );
      localStorage.setItem(SHORTCUTS_STORAGE_KEY, JSON.stringify(configs));
    } catch (error) {
      console.error('Failed to save custom shortcuts:', error);
    }
  }

  /**
   * Export shortcuts configuration
   */
  exportConfig(): ShortcutConfig[] {
    return this.getAllShortcuts().map(s => ({
      id: s.id,
      key: s.key,
      enabled: s.enabled,
    }));
  }

  /**
   * Import shortcuts configuration
   */
  importConfig(configs: ShortcutConfig[]): void {
    configs.forEach(config => {
      const shortcut = this.shortcuts.get(config.id);
      if (shortcut) {
        shortcut.key = config.key;
        shortcut.enabled = config.enabled;
        this.customShortcuts.set(config.id, config.key);
      }
    });
    this.saveCustomShortcuts();
  }
}
