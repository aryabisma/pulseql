/*
 * CloudBeaver - Pulsar Integration Plugin
 * Query History Panel Component
 */

import React, { useState } from 'react';
import { observer } from 'mobx-react-lite';
import { useService } from '@cloudbeaver/core-di';
import { QueryHistoryService, QueryHistoryEntry } from '../QueryHistoryService';

export const QueryHistoryPanel: React.FC = observer(() => {
  const queryHistoryService = useService(QueryHistoryService);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedEntry, setSelectedEntry] = useState<QueryHistoryEntry | null>(null);

  const recentQueries = searchTerm
    ? queryHistoryService.searchHistory(searchTerm)
    : queryHistoryService.recentQueries;

  const handleQuerySelect = (entry: QueryHistoryEntry) => {
    setSelectedEntry(entry);
    // Dispatch event for SQL editor to load query
    window.dispatchEvent(new CustomEvent('pulsar-load-query', {
      detail: { query: entry.query, connectionId: entry.connectionId },
    }));
  };

  const handleAddToFavorites = (entry: QueryHistoryEntry) => {
    const name = prompt('Enter a name for this favorite:');
    if (name) {
      queryHistoryService.historyToFavorite(entry.id, name, []);
    }
  };

  const handleRemoveEntry = (id: string) => {
    if (confirm('Remove this query from history?')) {
      queryHistoryService.removeHistoryEntry(id);
    }
  };

  const handleClearHistory = () => {
    if (confirm('Clear all query history? This cannot be undone.')) {
      queryHistoryService.clearHistory();
    }
  };

  const formatTimestamp = (timestamp: number) => {
    const date = new Date(timestamp);
    return date.toLocaleString();
  };

  const formatDuration = (ms?: number) => {
    if (!ms) return '-';
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(2)}s`;
  };

  return (
    <div className="query-history-panel">
      <div className="history-header">
        <h3>Query History</h3>
        <div className="history-actions">
          <input
            type="text"
            placeholder="Search history..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="history-search"
          />
          <button onClick={handleClearHistory} className="btn-clear">
            Clear All
          </button>
        </div>
      </div>

      <div className="history-list">
        {recentQueries.length === 0 ? (
          <div className="history-empty">
            <p>No query history yet</p>
            <p className="text-muted">Execute a query to see it here</p>
          </div>
        ) : (
          recentQueries.map((entry) => (
            <div
              key={entry.id}
              className={`history-entry ${selectedEntry?.id === entry.id ? 'selected' : ''}`}
              onClick={() => handleQuerySelect(entry)}
            >
              <div className="entry-header">
                <span className={`status-badge status-${entry.status}`}>
                  {entry.status}
                </span>
                <span className="entry-time">{formatTimestamp(entry.timestamp)}</span>
              </div>
              
              <div className="entry-query">
                <code>{entry.query.slice(0, 100)}{entry.query.length > 100 ? '...' : ''}</code>
              </div>
              
              <div className="entry-meta">
                <span className="meta-item">
                  <i className="icon-connection"></i>
                  {entry.connectionId}
                </span>
                {entry.executionTime && (
                  <span className="meta-item">
                    <i className="icon-clock"></i>
                    {formatDuration(entry.executionTime)}
                  </span>
                )}
                {entry.rowCount !== undefined && (
                  <span className="meta-item">
                    <i className="icon-table"></i>
                    {entry.rowCount} rows
                  </span>
                )}
              </div>

              <div className="entry-actions">
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    handleAddToFavorites(entry);
                  }}
                  className="btn-icon"
                  title="Add to favorites"
                >
                  <i className="icon-star"></i>
                </button>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    navigator.clipboard.writeText(entry.query);
                  }}
                  className="btn-icon"
                  title="Copy query"
                >
                  <i className="icon-copy"></i>
                </button>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    handleRemoveEntry(entry.id);
                  }}
                  className="btn-icon"
                  title="Remove from history"
                >
                  <i className="icon-trash"></i>
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
});
