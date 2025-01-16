package net.sanyal.fshandler.core;

import net.sanyal.fshandler.core.model.FileMetadata;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Predicate;

public interface FileSystem {
    /**
     * Creates a new file at the specified path
     * @param path Path where the file should be created
     * @return FileMetadata of the created file
     */
    FileMetadata createFile(String path);

    /**
     * Creates a new directory at the specified path
     * @param path Path where the directory should be created
     * @return FileMetadata of the created directory
     */
    FileMetadata createDirectory(String path);

    /**
     * Gets an input stream to read the file content
     * @param path Path to the file
     * @return InputStream for reading the file
     */
    InputStream readFile(String path);

    /**
     * Gets an output stream to write to the file
     * @param path Path to the file
     * @return OutputStream for writing to the file
     */
    OutputStream writeFile(String path);

    /**
     * Gets an output stream to append to the file
     * @param path Path to the file
     * @return OutputStream for appending to the file
     */
    OutputStream appendFile(String path);

    /**
     * Moves a file or directory from source to destination
     * @param sourcePath Source path
     * @param destinationPath Destination path
     * @return FileMetadata of the moved file/directory
     */
    FileMetadata move(String sourcePath, String destinationPath);

    /**
     * Renames a file or directory
     * @param path Current path
     * @param newName New name
     * @return FileMetadata of the renamed file/directory
     */
    FileMetadata rename(String path, String newName);

    /**
     * Deletes a file or directory
     * @param path Path to delete
     */
    void delete(String path);

    /**
     * Lists contents of a directory
     * @param path Directory path
     * @param filter Optional filter predicate
     * @return List of FileMetadata for directory contents
     */
    List<FileMetadata> list(String path, Predicate<FileMetadata> filter);

    /**
     * Recursively lists contents of a directory
     * @param path Directory path
     * @param filter Optional filter predicate
     * @return List of FileMetadata for all contents
     */
    List<FileMetadata> listRecursive(String path, Predicate<FileMetadata> filter);

    /**
     * Gets metadata for a file or directory
     * @param path Path to the file or directory
     * @return FileMetadata
     */
    FileMetadata getMetadata(String path);

    /**
     * Checks if the file system is healthy
     * @return true if healthy, false otherwise
     */
    boolean isHealthy();
}
