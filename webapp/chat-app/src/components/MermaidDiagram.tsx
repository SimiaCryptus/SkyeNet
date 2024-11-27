import React, {useEffect} from 'react';
import mermaid from 'mermaid';

const MermaidDiagram: React.FC<{ chart: string }> = ({ chart }) => {
    useEffect(() => {
        console.log('MermaidDiagram: Initializing with chart:', chart);

        mermaid.initialize({ startOnLoad: true });
        try {
            mermaid.contentLoaded();
            console.log('MermaidDiagram: Successfully rendered chart');
        } catch (error) {
            console.error('MermaidDiagram: Error rendering chart:', error);
        }
        return () => {
            console.log('MermaidDiagram: Cleanup');
        };
    }, [chart]);
    if (!chart) {
        console.warn('MermaidDiagram: No chart data provided');
        return null;
    }


    return <div className="mermaid">{chart}</div>;
};

export default MermaidDiagram;