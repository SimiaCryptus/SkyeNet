// Utility for safely interacting with localStorage
export const safeStorage = {
    setItem(key: string, value: string) {
        try {
            localStorage.setItem(key, value);
            return true;
        } catch (error: unknown) {
            console.warn('[Storage] Failed to save to localStorage:', {
                key,
                error,
                storageUsed: this.getUsedSpace()
            });
            // Try to clear old items if storage is full
            if (error instanceof Error && error.name === 'QuotaExceededError') {
                this.clearOldItems();
                try {
                    localStorage.setItem(key, value);
                    return true;
                } catch (retryError: unknown) {
                    console.error('[Storage] Still failed after clearing storage:', retryError);
                }
            }
            return false;
        }
    },

    getItem(key: string, defaultValue: string = '') {
        try {
            const value = localStorage.getItem(key);
            return value !== null ? value : defaultValue;
        } catch (error: unknown) {
            console.warn('[Storage] Failed to read from localStorage:', {
                key,
                error
            });
            return defaultValue;
        }
    },

    getUsedSpace() {
        try {
            let total = 0;
            for (let key in localStorage) {
                if (Object.prototype.hasOwnProperty.call(localStorage, key)) {
                    total += localStorage[key].length + key.length;
                }
            }
            return (total * 2) / 1024 / 1024; // Approximate MB used
        } catch (error: unknown) {
            console.error('[Storage] Failed to calculate storage usage:', error);
            return 0;
        }
    },

    clearOldItems() {
        try {
            const themeKey = 'theme';
            const verboseModeKey = 'verboseMode';
            // Keep important settings but clear other items
            const currentTheme = this.getItem(themeKey);
            const verboseMode = this.getItem(verboseModeKey);

            localStorage.clear();

            if (currentTheme) {
                this.setItem(themeKey, currentTheme);
            }
            if (verboseMode) {
                this.setItem(verboseModeKey, verboseMode);
            }
        } catch (error) {
            console.error('[Storage] Failed to clear storage:', error);
        }
    }
};