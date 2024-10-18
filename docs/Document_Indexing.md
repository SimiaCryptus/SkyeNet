# User Guide: Document Data Extraction and Query Index Creation

This guide covers two main features: Document Data Extraction and Query Index Creation. These tools are designed to help you extract structured data from various document types and
create searchable indexes for efficient querying.

## 1. Document Data Extraction

### Overview

The Document Data Extractor allows you to parse and extract structured information from PDF, TXT, MD, and HTML files. It uses AI to analyze the content and create a hierarchical
JSON representation of the document's structure, entities, and metadata.

### How to Use

1. In your IDE, right-click on a supported file (PDF, TXT, MD, or HTML) in the project explorer.
2. Select the "Document Data Extractor" option from the context menu.
3. A configuration dialog will appear with the following options:

- DPI: Set the resolution for image rendering (for PDFs).
- Max Pages: Limit the number of pages to process.
- Output Format: Choose the format for saved images (PNG, JPEG, GIF, BMP).
- Pages Per Batch: Set how many pages to process in each batch.
- Show Images: Toggle whether to display rendered images in the results.
- Save Image Files: Choose to save rendered images to disk.
- Save Text Files: Choose to save extracted text to disk.
- Save Final JSON: Choose to save the final parsed JSON to disk.

4. Click "OK" to start the extraction process.
5. A new browser window will open, showing the progress and results of the extraction.

### Output

- The extracted data will be displayed in the browser, organized by pages or batches.
- If enabled, image files, text files, and the final JSON will be saved in an "output" directory next to the source file.
- The final JSON file will have a ".parsed.json" extension.

## 2. Query Index Creation

### Overview

The Query Index Creator takes the parsed JSON files from the Document Data Extractor and creates a binary index file that can be efficiently searched using embedding-based
similarity search.

### How to Use

1. In your IDE, select one or more ".parsed.json" files in the project explorer.
2. Right-click and choose the "Save As Query Index" option from the context menu.
3. A file chooser dialog will appear. Select the directory where you want to save the index file.
4. Click "OK" to start the conversion process.
5. A progress bar will show the status of the index creation.

### Output

- A binary index file named "document.index.data" will be created in the selected output directory.
- This index file can be used for fast similarity searches on the extracted document data.

## Using the Query Index

Once you have created the query index, you can use it with the EmbeddingSearchTask to perform similarity searches on your document data. This allows you to quickly find relevant
information across all your indexed documents.

To use the EmbeddingSearchTask:

1. Set up your search query and parameters (e.g., distance type, number of results).
2. Point the task to your "document.index.data" file.
3. Run the search to get the most relevant results based on embedding similarity.

## Tips and Best Practices

1. For large documents, consider processing them in smaller batches by adjusting the "Max Pages" and "Pages Per Batch" settings.
2. Save the final JSON files when extracting data, as these are required to create the query index.
3. Organize your parsed JSON files in a dedicated folder to make it easier to select them when creating the query index.
4. When creating the query index, choose an output location that is easily accessible for your search tasks.
5. Experiment with different DPI settings for PDFs to balance image quality and processing speed.
6. Use the "Show Images" option during extraction to visually verify the content being processed, especially for PDFs.

