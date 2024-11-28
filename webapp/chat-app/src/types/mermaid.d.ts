declare module 'mermaid' {
    export interface MermaidAPI {
        initialize: (config: any) => void;
        render: (id: string, text: string) => void;
        run: () => void;
        parse: (text: string) => void;
        parseError?: Error;
    }

    const mermaid: MermaidAPI;
    export default mermaid;
    export type {MermaidAPI};
}