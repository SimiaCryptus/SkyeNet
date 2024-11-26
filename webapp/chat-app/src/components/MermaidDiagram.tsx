import React, { useEffect } from 'react';
import mermaid from 'mermaid';

const MermaidDiagram: React.FC<{ chart: string }> = ({ chart }) => {
    useEffect(() => {
        mermaid.initialize({ startOnLoad: true });
        mermaid.contentLoaded();
    }, [chart]);

    return <div className="mermaid">{chart}</div>;
};

export default MermaidDiagram;