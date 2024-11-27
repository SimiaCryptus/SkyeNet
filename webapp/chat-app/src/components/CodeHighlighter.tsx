import React, {useEffect} from 'react';
import Prism from 'prismjs';
import 'prismjs/components/prism-jsx';

const CodeHighlighter: React.FC<{ code: string }> = ({ code }) => {
    console.log('CodeHighlighter: Rendering with code length:', code.length);

    useEffect(() => {
        console.log('CodeHighlighter: Running highlight effect');
        Prism.highlightAll();
        return () => {
            console.log('CodeHighlighter: Cleanup effect');
        };
    }, [code]);
    console.log('CodeHighlighter: Returning JSX');

    return (
        <pre>
            <code className="language-jsx">{code}</code>
        </pre>
    );
};
// Log when the component is first loaded
console.log('CodeHighlighter: Component loaded');


export default CodeHighlighter;