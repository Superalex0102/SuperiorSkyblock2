package com.bgsoftware.superiorskyblock.island.container.cache;

import com.bgsoftware.superiorskyblock.api.island.Island;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Optional;

public final class IslandsCacheFile {

    private static final int HEADER_SIZE = 4;
    private static final String HEADER_MAGIC = "\\x02ICF";

    private final RandomAccessFile randomAccessFile;

    public IslandsCacheFile(File file) throws IOException {
        this.randomAccessFile = new RandomAccessFile(file, "rw");
        checkHeaders();
    }

    private void checkHeaders() throws IOException {
        byte[] fileHeaderBytes = new byte[HEADER_SIZE];
        this.randomAccessFile.seek(0);
        this.randomAccessFile.readFully(fileHeaderBytes);
        String fileHeader = new String(fileHeaderBytes);
        if (!fileHeader.equals(HEADER_MAGIC))
            throw new IOException("File is not a valid cache file.");
    }

    public Optional<Island> readIsland(int position) {
        try {
            int islandsDescriptorTableLength = this.randomAccessFile.readInt();

            if (islandsDescriptorTableLength < position)
                return Optional.empty();

            this.randomAccessFile.skipBytes((position - 1) * 4);
            int islandOffset = this.randomAccessFile.readInt();

            return readIslandEntry(islandOffset);
        } catch (IOException error) {
            return Optional.empty();
        }
    }

    public boolean saveIsland(Island island) {
        try {
            int islandId = island.getSessionId();
            int islandEntryOffset = saveIslandId(islandId);

            this.randomAccessFile.seek(HEADER_SIZE);
            int islandsDescriptorTableLength = this.randomAccessFile.readInt();

            // Making sure the offset is not less than the headers + IDT
            if (islandEntryOffset <= HEADER_SIZE + islandsDescriptorTableLength * 4)
                return false;

            return saveIslandEntry(islandId);
        } catch (IOException error) {
            return false;
        }
    }

    private int saveIslandId(int islandId) throws IOException {
        this.randomAccessFile.seek(HEADER_SIZE);
        int islandsDescriptorTableLength = this.randomAccessFile.readInt();
        int islandOffset;

        /*
        If the id of the island is bigger than the length of the IDT, we need to insert the new id at
        the correct area in the file, append the contents correctly and save the end of the file as
        the area where to write the island's data.
         */

        if (islandId <= islandsDescriptorTableLength) {
            this.randomAccessFile.skipBytes((islandId - 1) * 4);
            islandOffset = this.randomAccessFile.readInt();
        } else {
            // Changing the IDT length to the new length.
            this.randomAccessFile.seek(HEADER_SIZE);
            this.randomAccessFile.writeInt(islandId);
            // Inserting paddings to our id.
            this.randomAccessFile.skipBytes(islandsDescriptorTableLength * 4);
            for (int i = islandsDescriptorTableLength + 1; i < islandId; ++i)
                insertInt(this.randomAccessFile, 0);
            // Inserting the file's size to the node of the id (+4 as the node wasn't written yet)
            islandOffset = (int) this.randomAccessFile.length() + 4;
            insertInt(this.randomAccessFile, islandOffset);
        }

        return islandOffset;
    }

    private Optional<Island> readIslandEntry(int islandOffset) throws IOException {
        // Move file-pointer to the entry
        this.randomAccessFile.seek(islandOffset);

        return Optional.empty();
    }

    private boolean saveIslandEntry(int islandOffset) throws IOException {
        // Move file-pointer to the entry
        this.randomAccessFile.seek(islandOffset);

        return true;
    }

    private static void insertInt(RandomAccessFile randomAccessFile, int value) throws IOException {
        long writingPoint = randomAccessFile.getFilePointer();
        byte[] fileContents = new byte[(int) (randomAccessFile.length() - writingPoint)];
        randomAccessFile.readFully(fileContents);

        randomAccessFile.seek(writingPoint);
        randomAccessFile.writeInt(value);
        randomAccessFile.write(fileContents);
        // Move file-pointer to where the original value was written to
        randomAccessFile.seek(writingPoint + 4);
    }

}
