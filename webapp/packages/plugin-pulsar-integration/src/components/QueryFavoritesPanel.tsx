/*
 * CloudBeaver - Pulsar Integration Plugin
 * Query Favorites Panel Component
 */

import React, { useState } from 'react';
import { observer } from 'mobx-react-lite';
import { useService } from '@cloudbeaver/core-di';
import { QueryHistoryService, QueryFavorite } from '../QueryHistoryService';

export const QueryFavoritesPanel: React.FC = observer(() => {
  const queryHistoryService = useService(QueryHistoryService);
  const [selectedTag, setSelectedTag] = useState<string | null>(null);
  const [editingFavorite, setEditingFavorite] = useState<QueryFavorite | null>(null);

  const favorites = selectedTag
    ? queryHistoryService.getFavoritesByTag(selectedTag)
    : queryHistoryService.favoriteQueries;

  // Get all unique tags
  const allTags = Array.from(
    new Set(queryHistoryService.favoriteQueries.flatMap(f => f.tags))
  );

  const handleLoadQuery = (favorite: QueryFavorite) => {
    window.dispatchEvent(new CustomEvent('pulsar-load-query', {
      detail: { query: favorite.query, connectionId: favorite.connectionId },
    }));
  };

  const handleEditFavorite = (favorite: QueryFavorite) => {
    setEditingFavorite(favorite);
  };

  const handleSaveEdit = () => {
    if (editingFavorite) {
      const name = prompt('Enter new name:', editingFavorite.name);
      if (name) {
        queryHistoryService.updateFavorite(editingFavorite.id, { name });
      }
      setEditingFavorite(null);
    }
  };

  const handleRemoveFavorite = (id: string) => {
    if (confirm('Remove this favorite?')) {
      queryHistoryService.removeFavorite(id);
    }
  };

  const handleExport = () => {
    const data = queryHistoryService.exportData();
    const blob = new Blob([JSON.stringify(data, null, 2)], {
      type: 'application/json',
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `query-favorites-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="query-favorites-panel">
      <div className="favorites-header">
        <h3>Favorite Queries</h3>
        <button onClick={handleExport} className="btn-export">
          Export
        </button>
      </div>

      <div className="favorites-tags">
        <button
          onClick={() => setSelectedTag(null)}
          className={`tag-btn ${selectedTag === null ? 'active' : ''}`}
        >
          All ({queryHistoryService.favoriteQueries.length})
        </button>
        {allTags.map(tag => (
          <button
            key={tag}
            onClick={() => setSelectedTag(tag)}
            className={`tag-btn ${selectedTag === tag ? 'active' : ''}`}
          >
            {tag} ({queryHistoryService.getFavoritesByTag(tag).length})
          </button>
        ))}
      </div>

      <div className="favorites-list">
        {favorites.length === 0 ? (
          <div className="favorites-empty">
            <p>No favorites yet</p>
            <p className="text-muted">
              {selectedTag
                ? `No favorites with tag "${selectedTag}"`
                : 'Add queries to favorites from history'}
            </p>
          </div>
        ) : (
          favorites.map((favorite) => (
            <div key={favorite.id} className="favorite-entry">
              <div className="favorite-header">
                <h4>{favorite.name}</h4>
                <div className="favorite-meta">
                  <span className="meta-date">
                    {new Date(favorite.createdAt).toLocaleDateString()}
                  </span>
                </div>
              </div>

              <div className="favorite-query">
                <code>{favorite.query}</code>
              </div>

              {favorite.tags.length > 0 && (
                <div className="favorite-tags">
                  {favorite.tags.map(tag => (
                    <span key={tag} className="tag">
                      {tag}
                    </span>
                  ))}
                </div>
              )}

              <div className="favorite-actions">
                <button
                  onClick={() => handleLoadQuery(favorite)}
                  className="btn-primary"
                >
                  Load Query
                </button>
                <button
                  onClick={() => handleEditFavorite(favorite)}
                  className="btn-icon"
                  title="Edit favorite"
                >
                  <i className="icon-edit"></i>
                </button>
                <button
                  onClick={() => navigator.clipboard.writeText(favorite.query)}
                  className="btn-icon"
                  title="Copy query"
                >
                  <i className="icon-copy"></i>
                </button>
                <button
                  onClick={() => handleRemoveFavorite(favorite.id)}
                  className="btn-icon"
                  title="Remove favorite"
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
