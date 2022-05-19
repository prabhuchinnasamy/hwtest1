package com.serenegiant.common;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2022 saki t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/

import android.content.Context;
import android.graphics.Bitmap;
import android.view.Surface;

import com.serenegiant.glutils.GLManager;
import com.serenegiant.glutils.ImageTextureSource;
import com.serenegiant.glutils.SurfaceReader;
import com.serenegiant.graphics.BitmapHelper;
import com.serenegiant.math.Fraction;
import com.serenegiant.utils.HandlerThreadHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.serenegiant.common.TestUtils.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ImageTextureSourceTest {
	private static final String TAG = ImageTextureSourceTest.class.getSimpleName();

	private static final int WIDTH = 100;
	private static final int HEIGHT = 100;

	@Before
	public void prepare() {
		final Context context = ApplicationProvider.getApplicationContext();
	}

	@After
	public void cleanUp() {
		final Context context = ApplicationProvider.getApplicationContext();
	}

	@Test
	public void imageTextureSource() {
		final Bitmap original = BitmapHelper.makeCheckBitmap(
			WIDTH, HEIGHT, 15, 12, Bitmap.Config.ARGB_8888);
//		dump(bitmap);

		// 映像ソース用にImageTextureSourceを生成
		final GLManager manager = new GLManager();
		final ImageTextureSource source = new ImageTextureSource(manager, original, new Fraction(30));

		// 映像受け取り用にSurfaceReaderを生成
		final Semaphore sem = new Semaphore(0);
		final AtomicReference<Bitmap> result = new AtomicReference<>();
		final SurfaceReader reader = new SurfaceReader(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888, 2);
		reader.setOnImageAvailableListener(new SurfaceReader.OnImageAvailableListener() {
			final AtomicInteger cnt = new AtomicInteger();
			@Override
			public void onImageAvailable(@NonNull final SurfaceReader reader) {
				final Bitmap bitmap = reader.acquireLatestImage();
				if (bitmap != null) {
					try {
						if (cnt.incrementAndGet() >= 30) {
							source.setSurface(null);
							if (sem.availablePermits() == 0) {
								result.set(Bitmap.createBitmap(bitmap));
								sem.release();
							}
						}
					} finally {
						reader.recycle(bitmap);
					}
				}
			}
		}, HandlerThreadHandler.createHandler());
		final Surface surface = reader.getSurface();
		assertNotNull(surface);

		// 映像受け取り用Surfaceをセット
		source.setSurface(surface);
		try {
			// 30fpsなので約1秒以内に抜けてくるはず(多少の遅延・タイムラグを考慮して少し長めに)
			assertTrue(sem.tryAcquire(1100, TimeUnit.MILLISECONDS));
			final Bitmap b = result.get();
//			dump(b);
			assertNotNull(b);
			// 元のビットマップと同じかどうかを検証
			assertTrue(bitMapEquals(original, b));
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}
}