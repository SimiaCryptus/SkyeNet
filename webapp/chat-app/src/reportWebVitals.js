const reportWebVitals = (onPerfEntry) => {
    console.log('reportWebVitals called with:', onPerfEntry);
    if (onPerfEntry && onPerfEntry instanceof Function) {
        console.log('Loading web-vitals module...');
        import('web-vitals').then(({getCLS, getFID, getFCP, getLCP, getTTFB}) => {
            console.log('Web-vitals loaded successfully');
            // Core Web Vitals
            getCLS(onPerfEntry);
            getFID(onPerfEntry);
            getFCP(onPerfEntry);
            getLCP(onPerfEntry);
            getTTFB(onPerfEntry);
            console.log('All web vital metrics initialized');
        }).catch(error => {
            console.error('Failed to load web-vitals:', error);
        });
    } else {
        console.warn('reportWebVitals: Invalid or missing onPerfEntry function');
    }
};

export default reportWebVitals;