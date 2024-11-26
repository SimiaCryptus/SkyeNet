// Extend the Window interface to include the mermaid property
interface Window {
    mermaid: import('mermaid').Mermaid;
    QRCode: typeof import('qrcode-generator');
}