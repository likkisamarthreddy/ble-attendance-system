/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *  In-Process TTL Cache — Zero-dependency, zero-cost
 *  Replaces Redis for hot-path DB queries
 *  Safe for single-process or cluster (per-worker cache)
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 */

const DEFAULT_TTL_MS = 5 * 60 * 1000; // 5 minutes
const CLEANUP_INTERVAL_MS = 60 * 1000; // Sweep expired entries every 60s

class TTLCache {
  constructor(name, ttlMs = DEFAULT_TTL_MS) {
    this.name = name;
    this.ttlMs = ttlMs;
    this.store = new Map();
    this.hits = 0;
    this.misses = 0;

    // Periodic cleanup of expired entries
    this._cleaner = setInterval(() => {
      const now = Date.now();
      let cleaned = 0;
      for (const [key, entry] of this.store) {
        if (now > entry.exp) {
          this.store.delete(key);
          cleaned++;
        }
      }
      if (cleaned > 0) {
        console.log(`[CACHE:${this.name}] 🧹 Cleaned ${cleaned} expired entries. Size: ${this.store.size}`);
      }
    }, CLEANUP_INTERVAL_MS);

    // Don't prevent process from exiting
    if (this._cleaner.unref) this._cleaner.unref();
  }

  /**
   * Get a cached value. Returns undefined if expired or missing.
   */
  get(key) {
    const entry = this.store.get(key);
    if (!entry) {
      this.misses++;
      return undefined;
    }
    if (Date.now() > entry.exp) {
      this.store.delete(key);
      this.misses++;
      return undefined;
    }
    this.hits++;
    return entry.data;
  }

  /**
   * Set a value with TTL.
   */
  set(key, data, customTtlMs) {
    this.store.set(key, {
      data,
      exp: Date.now() + (customTtlMs || this.ttlMs),
    });
  }

  /**
   * Invalidate a specific key (use after updates).
   */
  invalidate(key) {
    this.store.delete(key);
  }

  /**
   * Clear entire cache.
   */
  clear() {
    this.store.clear();
  }

  /**
   * Get cache statistics.
   */
  stats() {
    const total = this.hits + this.misses;
    return {
      name: this.name,
      size: this.store.size,
      hits: this.hits,
      misses: this.misses,
      hitRate: total > 0 ? ((this.hits / total) * 100).toFixed(1) + "%" : "N/A",
    };
  }
}

// ─── Pre-built caches for hot paths ──────────────────────────────

// Student lookups (by uid) — profiles rarely change mid-session
const studentCache = new TTLCache("students", 5 * 60 * 1000); // 5 min

// Course lookups (by id) — course data is near-static
const courseCache = new TTLCache("courses", 10 * 60 * 1000); // 10 min

// Attendance record lookups (by id) — changes on every mark, short TTL
const attendanceCache = new TTLCache("attendance", 3 * 1000); // 3 sec (just dedup within burst)

module.exports = {
  TTLCache,
  studentCache,
  courseCache,
  attendanceCache,
};
