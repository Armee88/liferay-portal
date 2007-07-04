/**
 * Copyright (c) 2000-2007 Liferay, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.spring.hibernate;

import com.liferay.portal.kernel.util.StringMaker;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.util.ClusterPool;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.ArrayUtil;
import com.liferay.util.GetterUtil;

import com.opensymphony.oscache.base.NeedsRefreshException;
import com.opensymphony.oscache.general.GeneralCacheAdministrator;

/**
 * <a href="FinderCache.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 *
 */
public class FinderCache {

	public static final boolean CACHE_ENABLED = GetterUtil.getBoolean(
		PropsUtil.get(PropsUtil.VALUE_OBJECT_FINDER_CACHE_ENABLED), true);

	public static final String GROUP_NAME = FinderCache.class.getName();

	public static final String[] GROUP_NAME_ARRAY = new String[] {GROUP_NAME};

	public static void clearCache() {
		_cache.flushGroup(GROUP_NAME);
	}

	public static void clearCache(String className) {
		String classNameGroupKey = _encodeKey(className);

		_cache.flushGroup(classNameGroupKey);
	}

	public static Object getResult(
		String className, String methodName, String[] params, Object[] args) {

		Object result = null;

		String key = _encodeKey(className, methodName, params, args);

		try {
			result = _cache.getFromCache(key);
		}
		catch (NeedsRefreshException nre) {
			result = null;
		}
		finally {
			if (result == null) {
				_cache.cancelUpdate(key);
			}
		}

		return result;
	}

	public static Object putResult(
		String className, String methodName, String[] params, Object[] args,
		Object result) {

		if (CACHE_ENABLED && CacheRegistry.isActive() && (result != null)) {
			StringMaker sm = new StringMaker();

			sm.append(PropsUtil.VALUE_OBJECT_FINDER_CACHE_ENABLED);
			sm.append(StringPool.PERIOD);
			sm.append(className);

			boolean classNameCacheEnabled = GetterUtil.getBoolean(
				PropsUtil.get(sm.toString()), true);

			if (classNameCacheEnabled) {
				String key = _encodeKey(className, methodName, params, args);

				String classNameGroupKey = _encodeKey(className);

				String[] groups = ArrayUtil.append(
					GROUP_NAME_ARRAY, classNameGroupKey);

				_cache.putInCache(key, result, groups);
			}
		}

		return result;
	}

	private static String _encodeKey(String className) {
		StringMaker sm = new StringMaker();

		sm.append(GROUP_NAME);
		sm.append(StringPool.POUND);
		sm.append(className);

		return sm.toString();
	}

	private static String _encodeKey(
		String className, String methodName, String[] params, Object[] args) {

		StringMaker sm = new StringMaker();

		sm.append(GROUP_NAME);
		sm.append(StringPool.POUND);
		sm.append(className);
		sm.append(StringPool.POUND);
		sm.append(methodName);
		sm.append(_PARAMS_SEPARATOR);

		for (int i = 0; i < params.length; i++) {
			String param = params[i];

			sm.append(StringPool.POUND);
			sm.append(param);
		}

		sm.append(_ARGS_SEPARATOR);

		for (int i = 0; i < args.length; i++) {
			Object arg = args[i];

			sm.append(StringPool.POUND);
			sm.append(arg.toString());
		}

		return sm.toString();
	}

	private static final String _ARGS_SEPARATOR = "_ARGS_SEPARATOR_";

	private static final String _PARAMS_SEPARATOR = "_PARAMS_SEPARATOR_";

	private static GeneralCacheAdministrator _cache = ClusterPool.getCache();

}