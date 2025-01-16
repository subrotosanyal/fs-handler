# File System Handler

A robust and extensible file system abstraction layer that supports both local file system and S3 storage backends.

## Features

- Abstraction layer for file system operations
- Support for local file system and Amazon S3
- REST API for all operations
- Comprehensive file metadata
- Health monitoring
- Configurable through properties
- Logging and error handling

## Supported Operations

- Create files and directories
- Read and write files
- Append to files (local file system only)
- Move and rename files/directories
- Delete files/directories
- List directory contents (with optional recursion)
- Get file metadata
- Health checks

## Getting Started

### Prerequisites

- Java 17 or higher
- Gradle 8.0 or higher
- AWS credentials (for S3 storage)

### Configuration

The application can be configured through `application.properties`:

```properties
# Choose storage backend (local or s3)
filesystem.type=local
filesystem.basePath=/path/to/storage

# S3 Configuration (if using S3)
filesystem.s3.region=us-west-2
filesystem.s3.accessKey=your-access-key
filesystem.s3.secretKey=your-secret-key
```

### Building

```bash
./gradlew clean build
```

### Running

```bash
./gradlew bootRun
```

Or run the JAR directly:

```bash
java -jar build/libs/fs-handler-1.0.jar
```

## API Endpoints

### File Operations

- `POST /api/v1/fs/file?path={path}` - Create a file
- `POST /api/v1/fs/directory?path={path}` - Create a directory
- `GET /api/v1/fs/file?path={path}` - Read a file
- `POST /api/v1/fs/file?path={path}` - Write to a file (multipart/form-data)
- `GET /api/v1/fs/list?path={path}&recursive={true|false}&filter={pattern}` - List directory contents
- `DELETE /api/v1/fs/delete?path={path}` - Delete a file/directory
- `PUT /api/v1/fs/move?sourcePath={source}&destinationPath={destination}` - Move a file/directory
- `PUT /api/v1/fs/rename?path={path}&newName={newName}` - Rename a file/directory
- `GET /api/v1/fs/health` - Check system health

### Response Format

All endpoints return a `FileMetadata` object (except health check) with the following structure:
```json
{
  "path": "string",
  "name": "string",
  "type": "FILE|DIRECTORY",
  "size": "number",
  "lastModified": "timestamp",
  "children": "number (for directories)"
}
```

### Error Handling

The API uses standard HTTP status codes:
- 200: Success
- 204: Success (for delete operations)
- 400: Bad Request (invalid path, invalid name)
- 404: Not Found
- 500: Internal Server Error

Error responses have the following format:
```
Error: <error message>
```

### Path Validation

All paths are validated to prevent:
- Null or empty paths
- Path traversal attacks (..)
- Invalid characters in file names (/ in new names)

## Path Validation Rules

The File System Handler enforces strict path validation rules to ensure security:

1. **Path Requirements**
   - Paths must not be null or empty
   - Only relative paths are allowed
   - Use "." to refer to the root directory
   - All paths are relative to the configured base directory (`filesystem.basePath`)

2. **Prohibited Paths**
   - Absolute paths (starting with "/" or "\\")
   - Windows-style absolute paths (e.g., "C:\\folder")
   - Path traversal attempts (containing "..")
   - Mixed separator path traversal (e.g., "folder/../..\\etc")

3. **Examples**
   - Valid paths:
     ```
     .
     folder
     folder/subfolder
     folder/file.txt
     ```
   - Invalid paths:
     ```
     /
     /etc
     \temp
     C:\Windows
     ../folder
     folder/../../etc
     ```

4. **Error Handling**
   - Invalid paths will result in a 400 Bad Request response
   - Error message: "Invalid path: absolute paths and path traversal not allowed"
   - All file operations (list, create, read, write, delete) enforce these rules

5. **Security Notes**
   - All paths are normalized before processing
   - The base directory (`filesystem.basePath`) acts as a chroot-like environment
   - No access is allowed outside the base directory
   - Both Unix and Windows path separators are checked

## Docker Support

The application can be run as a Docker container. Both Dockerfile and docker-compose.yml are provided.

### Building and Running with Docker

1. Build the Docker image:
   ```bash
   docker build -t fs-handler .
   ```

2. Run the container:
   ```bash
   docker run -p 8080:8080 -v fs-data:/data/fs-handler fs-handler
   ```

### Using Docker Compose

1. Start the service:
   ```bash
   docker-compose up -d
   ```

2. View logs:
   ```bash
   docker-compose logs -f
   ```

3. Stop the service:
   ```bash
   docker-compose down
   ```

### Configuration

The following environment variables can be configured:

| Variable | Description | Default |
|----------|-------------|---------|
| SERVER_PORT | Application port | 8080 |
| FILESYSTEM_TYPE | Storage type (local/s3) | local |
| FILESYSTEM_BASEPATH | Base path for storage | /data/fs-handler |
| FILESYSTEM_ROOT | Root directory | . |

### Volumes

- `fs-data`: Persistent volume for storing files
- Mount point: `/data/fs-handler`

### Health Check

The container includes a health check that:
- Pings `/api/v1/fs/health` endpoint
- Runs every 30 seconds
- Has a 40-second startup grace period
- Retries 3 times before marking unhealthy

## Security Considerations

- Path traversal protection
- Configurable through Spring Security (not included)
- AWS credentials management for S3

## Performance

- Buffered I/O operations
- Streaming for large files
- Configurable connection pools and timeouts

## Error Handling

- Comprehensive error messages
- Proper HTTP status codes
- Detailed logging

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
