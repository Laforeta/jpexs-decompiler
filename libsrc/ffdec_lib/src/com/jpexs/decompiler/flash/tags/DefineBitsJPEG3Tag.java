/*
 *  Copyright (C) 2010-2015 JPEXS, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.jpexs.decompiler.flash.tags;

import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.SWFInputStream;
import com.jpexs.decompiler.flash.SWFOutputStream;
import com.jpexs.decompiler.flash.configuration.Configuration;
import com.jpexs.decompiler.flash.helpers.ImageHelper;
import com.jpexs.decompiler.flash.tags.base.AloneTag;
import com.jpexs.decompiler.flash.tags.base.ImageTag;
import com.jpexs.decompiler.flash.tags.enums.ImageFormat;
import com.jpexs.decompiler.flash.types.BasicType;
import com.jpexs.decompiler.flash.types.annotations.SWFType;
import com.jpexs.helpers.ByteArrayRange;
import com.jpexs.helpers.SerializableImage;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author JPEXS
 */
public class DefineBitsJPEG3Tag extends ImageTag implements AloneTag {

    public static final int ID = 35;

    public static final String NAME = "DefineBitsJPEG3";

    @SWFType(BasicType.UI8)
    public ByteArrayRange imageData;

    @SWFType(BasicType.UI8)
    public ByteArrayRange bitmapAlphaData;

    /**
     * Constructor
     *
     * @param swf
     */
    public DefineBitsJPEG3Tag(SWF swf) {
        super(swf, ID, NAME, null);
        characterID = swf.getNextCharacterId();
        imageData = new ByteArrayRange(createEmptyImage());
        bitmapAlphaData = ByteArrayRange.EMPTY;
        forceWriteAsLong = true;
    }

    /**
     * Constructor
     *
     * @param sis
     * @param data
     * @throws IOException
     */
    public DefineBitsJPEG3Tag(SWFInputStream sis, ByteArrayRange data) throws IOException {
        super(sis.getSwf(), ID, NAME, data);
        readData(sis, data, 0, false, false, false);
    }

    @Override
    public final void readData(SWFInputStream sis, ByteArrayRange data, int level, boolean parallel, boolean skipUnusualTags, boolean lazy) throws IOException {
        characterID = sis.readUI16("characterID");
        long alphaDataOffset = sis.readUI32("alphaDataOffset");
        imageData = sis.readByteRangeEx(alphaDataOffset, "imageData");
        bitmapAlphaData = sis.readByteRangeEx(sis.available(), "bitmapAlphaData");
    }

    /**
     * Gets data bytes
     *
     * @return Bytes of data
     */
    @Override
    public byte[] getData() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream os = baos;
        SWFOutputStream sos = new SWFOutputStream(os, getVersion());
        try {
            sos.writeUI16(characterID);
            sos.writeUI32(imageData.getLength());
            sos.write(imageData);
            sos.write(bitmapAlphaData);
        } catch (IOException e) {
            throw new Error("This should never happen.", e);
        }
        return baos.toByteArray();
    }

    private byte[] createEmptyImage() {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream bitmapDataOS = new ByteArrayOutputStream();
        ImageHelper.write(img, ImageFormat.JPEG, bitmapDataOS);
        return bitmapDataOS.toByteArray();
    }

    @Override
    public void setImage(byte[] data) throws IOException {
        if (ImageTag.getImageFormat(data) == ImageFormat.JPEG) {
            BufferedImage image = ImageHelper.read(data);
            byte[] ba = new byte[image.getWidth() * image.getHeight()];
            for (int i = 0; i < ba.length; i++) {
                ba[i] = (byte) 255;
            }

            bitmapAlphaData = new ByteArrayRange(SWFOutputStream.compressByteArray(ba));
        } else {
            bitmapAlphaData = ByteArrayRange.EMPTY;
        }

        imageData = new ByteArrayRange(data);
        clearCache();
        setModified(true);
    }

    public void setImageAlpha(byte[] data) throws IOException {
        ImageFormat fmt = ImageTag.getImageFormat(imageData);
        if (fmt != ImageFormat.JPEG) {
            throw new IOException("Only Jpeg can have alpha channel.");
        }

        SerializableImage image = getImage();
        if (data == null || data.length != image.getWidth() * image.getHeight()) {
            throw new IOException("Data length must match the size of the image.");
        }

        bitmapAlphaData = new ByteArrayRange(SWFOutputStream.compressByteArray(data));
        clearCache();
        setModified(true);
    }

    @Override
    public ImageFormat getImageFormat() {
        ImageFormat fmt = ImageTag.getImageFormat(imageData);
        if (fmt == ImageFormat.JPEG) {
            fmt = ImageFormat.PNG; //transparency
        }
        return fmt;
    }

    @Override
    public InputStream getImageData() {
        int errorLength = hasErrorHeader(imageData) ? 4 : 0;
        return new ByteArrayInputStream(imageData.getArray(), imageData.getPos() + errorLength, imageData.getLength() - errorLength);
    }

    @Override
    public SerializableImage getImage() {
        if (cachedImage != null) {
            return cachedImage;
        }
        try {
            BufferedImage image = ImageHelper.read(getImageData());
            if (image == null) {
                Logger.getLogger(DefineBitsJPEG3Tag.class.getName()).log(Level.SEVERE, "Failed to load image");
                return null;
            }

            SerializableImage img = new SerializableImage(image);
            if (bitmapAlphaData.getLength() == 0) {
                if (Configuration.cacheImages.get()) {
                    cachedImage = img;
                }

                return img;
            }

            byte[] alphaData = SWFInputStream.uncompressByteArray(bitmapAlphaData.getRangeData());
            int[] pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
            for (int i = 0; i < pixels.length; i++) {
                int a = alphaData[i] & 0xff;
                pixels[i] = multiplyAlpha((pixels[i] & 0xffffff) | (a << 24));
            }

            if (Configuration.cacheImages.get()) {
                cachedImage = img;
            }

            return img;
        } catch (IOException ex) {
            Logger.getLogger(DefineBitsJPEG3Tag.class.getName()).log(Level.SEVERE, "Failed to get image", ex);
        }
        return null;
    }
}
