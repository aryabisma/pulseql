/*
 * CloudBeaver - Pulsar Integration Plugin
 * Query History & Favorites Service
 * 
 * Manages user query history and favorites for improved productivity
 */

import { injectable } from '@cloudbeaver/core-di';
import { makeObservable, observable, action, computed } from 'mobx';

export interface QueryHistoryEntry {
  id: string;
  query: string;
  connectionId: string;
  timestamp: number;
  executionTime?: number;
  rowCount?: number;
  status: 'success' | 'error' | 'cancelled';
  error?: string;
}

export interface QueryFavorite {
  id: string;
  name: string;
  query: string;
  connectionId?: string;
  tags: string[];
  createdAt: number;
  updatedAt: number;
}

const HISTORY_STORAGE_KEY = 'pulsar_query_history';
const FAVORITES_STORAGE_KEY = 'pulsar_query_favorites';
const MAX_HISTORY_ENTRIES = 100;

@injectable()
export class QueryHistoryService {
  private history: QueryHistoryEntry[] = [];
  private favorites: QueryFavorite[] = [];

  constructor() {
    makeObservable<this, 'history' | 'favorites'>(this, {
      history: observable,
      favorites: observable,
      addHistoryEntry: action,
      clearHistory: action,
      addFavorite: action,
      removeFavorite: action,
      updateFavorite: action,
      recentQueries: computed,
      favoriteQueries: computed,
    });

    this.loadFromStorage();
  }

  /**
   * Add a query to history
   */
  addHistoryEntry(entry: Omit<QueryHistoryEntry, 'id' | 'timestamp'>): void {
    const newEntry: QueryHistoryEntry = {
      ...entry,
      id: this.generateId(),
      timestamp: Date.now(),
    };

    this.history.unshift(newEntry);
    
    // Limit history size
    if (this.history.length > MAX_HISTORY_ENTRIES) {
      this.history = this.history.slice(0, MAX_HISTORY_ENTRIES);
    }

    this.saveToStorage();
  }

  /**
   * Get recent queries (last N entries)
   */
  get recentQueries(): QueryHistoryEntry[] {
    return this.history.slice(0, 20);
  }

  /**
   * Get history by connection
   */
  getHistoryByConnection(connectionId: string): QueryHistoryEntry[] {
    return this.history.filter(entry => entry.connectionId === connectionId);
  }

  /**
   * Search history
   */
  searchHistory(searchTerm: string): QueryHistoryEntry[] {
    const term = searchTerm.toLowerCase();
    return this.history.filter(entry => 
      entry.query.toLowerCase().includes(term)
    );
  }

  /**
   * Clear all history
   */
  clearHistory(): void {
    this.history = [];
    this.saveToStorage();
  }

  /**
   * Remove specific history entry
   */
  removeHistoryEntry(id: string): void {
    this.history = this.history.filter(entry => entry.id !== id);
    this.saveToStorage();
  }

  /**
   * Add a favorite query
   */
  addFavorite(favorite: Omit<QueryFavorite, 'id' | 'createdAt' | 'updatedAt'>): void {
    const newFavorite: QueryFavorite = {
      ...favorite,
      id: this.generateId(),
      createdAt: Date.now(),
      updatedAt: Date.now(),
    };

    this.favorites.push(newFavorite);
    this.saveToStorage();
  }

  /**
   * Get all favorites
   */
  get favoriteQueries(): QueryFavorite[] {
    return this.favorites;
  }

  /**
   * Get favorites by tag
   */
  getFavoritesByTag(tag: string): QueryFavorite[] {
    return this.favorites.filter(fav => fav.tags.includes(tag));
  }

  /**
   * Update a favorite
   */
  updateFavorite(id: string, updates: Partial<Omit<QueryFavorite, 'id' | 'createdAt'>>): void {
    const favorite = this.favorites.find(fav => fav.id === id);
    if (favorite) {
      Object.assign(favorite, updates, { updatedAt: Date.now() });
      this.saveToStorage();
    }
  }

  /**
   * Remove a favorite
   */
  removeFavorite(id: string): void {
    this.favorites = this.favorites.filter(fav => fav.id !== id);
    this.saveToStorage();
  }

  /**
   * Check if query is favorited
   */
  isFavorite(query: string): boolean {
    return this.favorites.some(fav => fav.query === query);
  }

  /**
   * Convert history entry to favorite
   */
  historyToFavorite(historyId: string, name: string, tags: string[] = []): void {
    const entry = this.history.find(h => h.id === historyId);
    if (entry) {
      this.addFavorite({
        name,
        query: entry.query,
        connectionId: entry.connectionId,
        tags,
      });
    }
  }

  /**
   * Export history/favorites
   */
  exportData(): { history: QueryHistoryEntry[]; favorites: QueryFavorite[] } {
    return {
      history: this.history,
      favorites: this.favorites,
    };
  }

  /**
   * Import history/favorites
   */
  importData(data: { history?: QueryHistoryEntry[]; favorites?: QueryFavorite[] }): void {
    if (data.history) {
      this.history = data.history;
    }
    if (data.favorites) {
      this.favorites = data.favorites;
    }
    this.saveToStorage();
  }

  // Private helpers

  private generateId(): string {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  private loadFromStorage(): void {
    try {
      const historyData = localStorage.getItem(HISTORY_STORAGE_KEY);
      const favoritesData = localStorage.getItem(FAVORITES_STORAGE_KEY);

      if (historyData) {
        this.history = JSON.parse(historyData);
      }
      if (favoritesData) {
        this.favorites = JSON.parse(favoritesData);
      }
    } catch (error) {
      console.error('Failed to load query history/favorites from storage:', error);
    }
  }

  private saveToStorage(): void {
    try {
      localStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify(this.history));
      localStorage.setItem(FAVORITES_STORAGE_KEY, JSON.stringify(this.favorites));
    } catch (error) {
      console.error('Failed to save query history/favorites to storage:', error);
    }
  }
}
