/**
 * Copyright (C) 2014-2018 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.peppol.smpserver.ui.secure;

import java.util.Locale;

import javax.annotation.Nonnull;

import com.helger.commons.url.ISimpleURL;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCSpan;
import com.helger.html.hc.html.textlevel.HCStrong;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.app.CSMP;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.security.SMPKeyManager;
import com.helger.peppol.smpserver.settings.ISMPSettings;
import com.helger.peppol.smpserver.ui.pub.SMPRendererPublic;
import com.helger.photon.bootstrap4.CBootstrapCSS;
import com.helger.photon.bootstrap4.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap4.alert.EBootstrapAlertType;
import com.helger.photon.bootstrap4.breadcrumb.BootstrapBreadcrumb;
import com.helger.photon.bootstrap4.breadcrumb.BootstrapBreadcrumbProvider;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.grid.BootstrapCol;
import com.helger.photon.bootstrap4.grid.BootstrapRow;
import com.helger.photon.bootstrap4.layout.BootstrapContainer;
import com.helger.photon.bootstrap4.navbar.BootstrapNavbar;
import com.helger.photon.bootstrap4.navbar.BootstrapNavbarToggleable;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapMenuItemRenderer;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.app.context.ILayoutExecutionContext;
import com.helger.photon.core.app.context.LayoutExecutionContext;
import com.helger.photon.core.app.layout.CLayout;
import com.helger.photon.core.servlet.AbstractPublicApplicationServlet;
import com.helger.photon.core.servlet.LogoutServlet;
import com.helger.photon.core.url.LinkHelper;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.util.SecurityHelper;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * The viewport renderer (menu + content area)
 *
 * @author Philip Helger
 */
public final class SMPRendererSecure
{
  private SMPRendererSecure ()
  {}

  @Nonnull
  private static IHCNode _getNavbar (@Nonnull final ILayoutExecutionContext aLEC)
  {
    final Locale aDisplayLocale = aLEC.getDisplayLocale ();
    final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();

    final ISimpleURL aLinkToStartPage = aLEC.getLinkToMenuItem (aLEC.getMenuTree ().getDefaultMenuItemID ());

    final BootstrapNavbar aNavbar = new BootstrapNavbar ();
    aNavbar.addBrand (SMPRendererPublic.createLogo (aLEC), aLinkToStartPage);
    aNavbar.addBrand (new HCSpan ().addChild (CSMP.getApplicationSuffix () + " Administration"), aLinkToStartPage);
    aNavbar.addAndReturnText ().addChild (" [" + SMPServerConfiguration.getSMLSMPID () + "]");

    final BootstrapNavbarToggleable aToggleable = aNavbar.addAndReturnToggleable ();

    {
      aToggleable.addChild (new BootstrapButton ().addClass (CBootstrapCSS.ML_AUTO)
                                                  .addClass (CBootstrapCSS.MR_2)
                                                  .setOnClick (LinkHelper.getURLWithContext (AbstractPublicApplicationServlet.SERVLET_DEFAULT_PATH +
                                                                                             "/"))
                                                  .addChild ("Goto public view"));

      final IUser aUser = LoggedInUserManager.getInstance ().getCurrentUser ();
      aToggleable.addAndReturnText ()
                 .addClass (CBootstrapCSS.MX_2)
                 .addChild ("Logged in as ")
                 .addChild (new HCStrong ().addChild (SecurityHelper.getUserDisplayName (aUser, aDisplayLocale)));
      aToggleable.addChild (new BootstrapButton ().addClass (CBootstrapCSS.MX_2)
                                                  .setOnClick (LinkHelper.getURLWithContext (aRequestScope,
                                                                                             LogoutServlet.SERVLET_DEFAULT_PATH))
                                                  .addChild (EPhotonCoreText.LOGIN_LOGOUT.getDisplayText (aDisplayLocale)));
    }
    return aNavbar;
  }

  @Nonnull
  public static IHCNode getMenuContent (@Nonnull final LayoutExecutionContext aLEC)
  {
    final HCNodeList ret = new HCNodeList ();
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();

    ret.addChild (BootstrapMenuItemRenderer.createSideBarMenu (aLEC));

    final BootstrapSuccessBox aBox = new BootstrapSuccessBox ();

    // Information on SML usage
    if (aSettings.isSMLActive ())
    {
      aBox.addChild (new HCDiv ().addChild (EDefaultIcon.YES.getAsNode ()).addChild (" SML connection is active."));
      if (SMPMetaManager.getSettings ().getSMLInfo () == null)
      {
        aBox.addChild (new HCDiv ().addChild (EDefaultIcon.NO.getAsNode ())
                                   .addChild (" No SML is selected. ")
                                   .addChild (new HCA (aLEC.getLinkToMenuItem (CMenuSecure.MENU_SMP_SETTINGS)).addChild ("Fix me")));
        aBox.setType (EBootstrapAlertType.DANGER);
      }
    }
    else
    {
      // Warn only if SML is needed
      if (SMPMetaManager.getSettings ().isSMLNeeded ())
      {
        aBox.addChild (new HCDiv ().addChild (EDefaultIcon.MINUS.getAsNode ())
                                   .addChild (" SML connection is NOT active. ")
                                   .addChild (new HCA (aLEC.getLinkToMenuItem (CMenuSecure.MENU_SMP_SETTINGS)).addChild ("Fix me")));
        aBox.setType (EBootstrapAlertType.WARNING);
      }
    }

    if (SMPServerConfiguration.getRESTType ().isPEPPOL ())
    {
      if (aSettings.isPEPPOLDirectoryIntegrationEnabled ())
      {
        aBox.addChild (new HCDiv ().addChild (EDefaultIcon.YES.getAsNode ())
                                   .addChild (" Directory support is enabled."));
      }
      else
      {
        aBox.addChild (new HCDiv ().addChild (EDefaultIcon.MINUS.getAsNode ())
                                   .addChild (" Directory support is disabled. ")
                                   .addChild (new HCA (aLEC.getLinkToMenuItem (CMenuSecure.MENU_SMP_SETTINGS)).addChild ("Fix me")));
        if (aBox.getType () == EBootstrapAlertType.SUCCESS)
          aBox.setType (EBootstrapAlertType.INFO);
      }
    }

    // Information on certificate
    if (!SMPKeyManager.isCertificateValid ())
    {
      aBox.addChild (new HCDiv ().addChild (EDefaultIcon.NO.getAsNode ())
                                 .addChild (" Certificate configuration is invalid. REST queries will not work!"));
      aBox.setType (EBootstrapAlertType.DANGER);
    }
    ret.addChild (aBox);

    return ret;
  }

  @Nonnull
  public static IHCNode getContent (@Nonnull final LayoutExecutionContext aLEC)
  {
    final HCNodeList ret = new HCNodeList ();

    final BootstrapContainer aOuterContainer = ret.addAndReturnChild (new BootstrapContainer ().setFluid (true));

    // Header
    aOuterContainer.addChild (_getNavbar (aLEC));

    // Breadcrumbs
    if (false)
    {
      final BootstrapBreadcrumb aBreadcrumbs = BootstrapBreadcrumbProvider.createBreadcrumb (aLEC);
      aBreadcrumbs.addClasses (CBootstrapCSS.D_NONE, CBootstrapCSS.D_SM_BLOCK);
      aOuterContainer.addChild (aBreadcrumbs);
    }

    // Content
    {
      final BootstrapRow aRow = aOuterContainer.addAndReturnChild (new BootstrapRow ());
      final BootstrapCol aCol1 = aRow.createColumn (12, 12, 4, 3, 2);
      final BootstrapCol aCol2 = aRow.createColumn (12, 12, 8, 9, 10);

      // left
      // We need a wrapper span for easy AJAX content replacement
      aCol1.addChild (new HCSpan ().setID (CLayout.LAYOUT_AREAID_MENU)
                                   .addClass (CBootstrapCSS.D_PRINT_NONE)
                                   .addChild (getMenuContent (aLEC)));
      aCol1.addChild (new HCDiv ().setID (CLayout.LAYOUT_AREAID_SPECIAL));

      // content - determine is exactly same as for view
      aCol2.addChild (SMPRendererPublic.getPageContent (aLEC));
    }

    aOuterContainer.addChild (SMPRendererPublic.createDefaultFooter ());

    return ret;
  }
}
