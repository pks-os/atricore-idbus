/*
 * Atricore IDBus
 *
 * Copyright (c) 2009, Atricore Inc.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.atricore.idbus.capabilities.samlr2.support.core;

/**
 * @author <a href=mailto:ajadzinsky@atricor.org>Alejandro Jadzinsky</a>
 *         User: ajadzinsky
 *         Date: Jun 9, 2009
 */
public class SamlR2KeyResolverException extends Exception {
    public SamlR2KeyResolverException() {
        super();
    }

    public SamlR2KeyResolverException(String message) {
        super(message);
    }

    public SamlR2KeyResolverException(String message, Throwable cause) {
        super(message, cause);
    }

    public SamlR2KeyResolverException(Throwable cause) {
        super(cause);
    }
}