/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.peppol.smpserver.domain;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.lang.ClassHelper;
import com.helger.commons.lang.ServiceLoaderHelper;
import com.helger.commons.scope.IScope;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;
import com.helger.peppol.smpserver.security.SMPKeyManager;

public final class MetaManager extends AbstractGlobalSingleton
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (MetaManager.class);

  private ISMPUserManager m_aUserMgr;
  private ISMPServiceGroupManager m_aServiceGroupMgr;
  private ISMPRedirectManager m_aRedirectMgr;
  private ISMPServiceInformationManager m_aServiceInformationMgr;

  @Deprecated
  @UsedViaReflection
  public MetaManager ()
  {}

  @Override
  protected void onAfterInstantiation (@Nonnull final IScope aScope)
  {
    try
    {
      final ISMPManagerProviderSPI aFactory = ServiceLoaderHelper.getFirstSPIImplementation (ISMPManagerProviderSPI.class);
      if (aFactory == null)
        throw new IllegalStateException ("Found no ISMPManagerProviderSPI implementation");

      m_aUserMgr = aFactory.createUserMgr ();
      if (m_aUserMgr == null)
        throw new IllegalStateException ("Failed to create User manager!");

      // Service group manager must be before redirect and service information!
      m_aServiceGroupMgr = aFactory.createServiceGroupMgr ();
      if (m_aServiceGroupMgr == null)
        throw new IllegalStateException ("Failed to create ServiceGroup manager!");
      m_aRedirectMgr = aFactory.createRedirectMgr ();
      if (m_aRedirectMgr == null)
        throw new IllegalStateException ("Failed to create Redirect manager!");
      m_aServiceInformationMgr = aFactory.createServiceInformationMgr ();
      if (m_aServiceInformationMgr == null)
        throw new IllegalStateException ("Failed to create ServiceInformation manager!");

      try
      {
        SMPKeyManager.getInstance ();
        SMPKeyManager.markCertificateValid ();
      }
      catch (final Exception ex)
      {
        // fall through. Certificate stays invalid
      }

      s_aLogger.info (ClassHelper.getClassLocalName (this) + " was initialized");
    }
    catch (final RuntimeException ex)
    {
      throw new InitializationException ("Failed to init " + ClassHelper.getClassLocalName (this), ex);
    }
  }

  @Nonnull
  public static MetaManager getInstance ()
  {
    return getGlobalSingleton (MetaManager.class);
  }

  @Nonnull
  public static ISMPUserManager getUserMgr ()
  {
    return getInstance ().m_aUserMgr;
  }

  @Nonnull
  public static ISMPServiceGroupManager getServiceGroupMgr ()
  {
    return getInstance ().m_aServiceGroupMgr;
  }

  @Nonnull
  public static ISMPRedirectManager getRedirectMgr ()
  {
    return getInstance ().m_aRedirectMgr;
  }

  @Nonnull
  public static ISMPServiceInformationManager getServiceInformationMgr ()
  {
    return getInstance ().m_aServiceInformationMgr;
  }
}
