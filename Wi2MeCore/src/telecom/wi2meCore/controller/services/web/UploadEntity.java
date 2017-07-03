/**
 * Copyright (c) 2012 Institut Mines-Telecom / Telecom Bretagne. All rights reserved.
 *
 * This file is part of Wi2Me.
 *
 * Wi2Me is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wi2Me is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Wi2Me.  If not, see <http://www.gnu.org/licenses/>.
 */

package telecom.wi2meCore.controller.services.web;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.InputStreamEntity;

	public class UploadEntity extends InputStreamEntity {

	    public UploadEntity(InputStream in, long length, IUploadProgressListener listener) {
			super(in, length);
			listener_ = listener;
		}

		private IUploadProgressListener listener_;
	    private CountingOutputStream outputStream_;
	    private OutputStream lastOutputStream_;


	    @Override
	    public void writeTo(OutputStream out) throws IOException {
	        // If we have yet to create the CountingOutputStream, or the
	        // OutputStream being passed in is different from the OutputStream used
	        // to create the current CountingOutputStream
	        if ((lastOutputStream_ == null) || (lastOutputStream_ != out)) {
	            lastOutputStream_ = out;
	            outputStream_ = new CountingOutputStream(out);
	        }

	        super.writeTo(outputStream_);
	    }

	    private class CountingOutputStream extends FilterOutputStream {

	        //private long transferred = 0;
	            private OutputStream wrappedOutputStream_;

	        public CountingOutputStream(final OutputStream out) {
	            super(out);
	                    wrappedOutputStream_ = out;
	        }

	        public void write(byte[] b, int off, int len) throws IOException {
	                    wrappedOutputStream_.write(b,off,len);
	                    //++transferred;
	            listener_.transferred(len);
	        }

	        public void write(int b) throws IOException {
	            super.write(b);
	        }
	    }
	}

