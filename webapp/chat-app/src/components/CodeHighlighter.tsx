import React, { useEffect } from 'react';
import Prism from 'prismjs';
import 'prismjs/components/prism-jsx';

const CodeHighlighter: React.FC<{ code: string }> = ({ code }) => {
    useEffect(() => {
        Prism.highlightAll();
    }, [code]);

    return (
        <pre>
            <code className="language-jsx">{code}</code>
        </pre>
    );
};

export default CodeHighlighter;