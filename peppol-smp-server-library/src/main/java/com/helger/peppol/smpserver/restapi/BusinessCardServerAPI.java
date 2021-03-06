/**
 * Copyright (C) 2015-2018 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.restapi;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.ESuccess;
import com.helger.commons.statistics.IMutableStatisticsHandlerKeyedCounter;
import com.helger.commons.statistics.IStatisticsHandlerKeyedCounter;
import com.helger.commons.statistics.StatisticsManager;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.pd.businesscard.generic.PDBusinessCard;
import com.helger.pd.businesscard.generic.PDBusinessEntity;
import com.helger.pd.businesscard.v3.PD3BusinessCardType;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCard;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCardManager;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardEntity;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.user.ISMPUser;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;
import com.helger.peppol.smpserver.exception.SMPBadRequestException;
import com.helger.peppol.smpserver.exception.SMPNotFoundException;
import com.helger.peppol.smpserver.exception.SMPServerException;

/**
 * This class implements all the service methods, that must be provided by the
 * BusinessCard REST service - this service is the same for BDXR and SMP.
 *
 * @author Philip Helger
 */
public final class BusinessCardServerAPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (BusinessCardServerAPI.class);
  private static final IMutableStatisticsHandlerKeyedCounter s_aStatsCounterInvocation = StatisticsManager.getKeyedCounterHandler (BusinessCardServerAPI.class.getName () +
                                                                                                                                   "$call");
  private static final IMutableStatisticsHandlerKeyedCounter s_aStatsCounterSuccess = StatisticsManager.getKeyedCounterHandler (BusinessCardServerAPI.class.getName () +
                                                                                                                                "$success");
  private static final IMutableStatisticsHandlerKeyedCounter s_aStatsCounterError = StatisticsManager.getKeyedCounterHandler (BusinessCardServerAPI.class.getName () +
                                                                                                                              "$error");
  private static final String LOG_PREFIX = "[BusinessCard REST API] ";

  private final ISMPServerAPIDataProvider m_aAPIProvider;

  public BusinessCardServerAPI (@Nonnull final ISMPServerAPIDataProvider aDataProvider)
  {
    m_aAPIProvider = ValueEnforcer.notNull (aDataProvider, "DataProvider");
  }

  @Nonnull
  public PD3BusinessCardType getBusinessCard (final String sServiceGroupID) throws SMPServerException
  {
    if (LOGGER.isInfoEnabled ())
      LOGGER.info (LOG_PREFIX + "GET /businesscard/" + sServiceGroupID);
    s_aStatsCounterInvocation.increment ("getBusinessCard");

    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
    if (aServiceGroupID == null)
    {
      // Invalid identifier
      throw new SMPBadRequestException ("Failed to parse serviceGroup '" + sServiceGroupID + "'",
                                        m_aAPIProvider.getCurrentURI ());
    }

    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aServiceGroupID);
    if (aServiceGroup == null)
    {
      // No such service group
      throw new SMPNotFoundException ("Unknown serviceGroup '" + sServiceGroupID + "'",
                                      m_aAPIProvider.getCurrentURI ());
    }

    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
    if (aBusinessCardMgr == null)
    {
      throw new SMPBadRequestException ("This SMP server does not support the BusinessCard API",
                                        m_aAPIProvider.getCurrentURI ());
    }
    final ISMPBusinessCard aBusinessCard = aBusinessCardMgr.getSMPBusinessCardOfServiceGroup (aServiceGroup);
    if (aBusinessCard == null)
    {
      // No such business card
      throw new SMPNotFoundException ("No BusinessCard assigned to serviceGroup '" + sServiceGroupID + "'",
                                      m_aAPIProvider.getCurrentURI ());
    }

    if (LOGGER.isInfoEnabled ())
      LOGGER.info (LOG_PREFIX + "Finished getBusinessCard(" + sServiceGroupID + ")");
    s_aStatsCounterSuccess.increment ("getBusinessCard");
    return aBusinessCard.getAsJAXBObject ();
  }

  @Nonnull
  public ESuccess createBusinessCard (@Nonnull final String sServiceGroupID,
                                      @Nonnull final PDBusinessCard aBusinessCard,
                                      @Nonnull final BasicAuthClientCredentials aCredentials) throws SMPServerException
  {
    if (LOGGER.isInfoEnabled ())
      LOGGER.info (LOG_PREFIX + "PUT /businesscard/" + sServiceGroupID + " ==> " + aBusinessCard);
    s_aStatsCounterInvocation.increment ("createBusinessCard");

    // Parse and validate identifier
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
    if (aServiceGroupID == null)
    {
      // Invalid identifier
      throw new SMPBadRequestException ("Failed to parse serviceGroup '" + sServiceGroupID + "'",
                                        m_aAPIProvider.getCurrentURI ());
    }

    final IParticipantIdentifier aPayloadServiceGroupID = aIdentifierFactory.createParticipantIdentifier (aBusinessCard.getParticipantIdentifier ()
                                                                                                                       .getScheme (),
                                                                                                          aBusinessCard.getParticipantIdentifier ()
                                                                                                                       .getValue ());
    if (!aServiceGroupID.hasSameContent (aPayloadServiceGroupID))
    {
      // Business identifiers must be equal
      throw new SMPBadRequestException ("Participant Inconsistency. The URL points to " +
                                        aServiceGroupID.getURIEncoded () +
                                        " whereas the BusinessCard contains " +
                                        aPayloadServiceGroupID.getURIEncoded (),
                                        m_aAPIProvider.getCurrentURI ());
    }

    // Retrieve the service group
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aServiceGroupID);
    if (aServiceGroup == null)
    {
      // No such service group (on this server)
      throw new SMPNotFoundException ("Unknown serviceGroup '" + sServiceGroupID + "'",
                                      m_aAPIProvider.getCurrentURI ());
    }

    // Check credentials and verify service group is owned by provided user
    final ISMPUserManager aUserMgr = SMPMetaManager.getUserMgr ();
    final ISMPUser aSMPUser = aUserMgr.validateUserCredentials (aCredentials);
    aUserMgr.verifyOwnership (aServiceGroupID, aSMPUser);

    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
    if (aBusinessCardMgr == null)
    {
      throw new SMPBadRequestException ("This SMP server does not support the BusinessCard API",
                                        m_aAPIProvider.getCurrentURI ());
    }

    final ICommonsList <SMPBusinessCardEntity> aEntities = new CommonsArrayList <> ();
    for (final PDBusinessEntity aEntity : aBusinessCard.businessEntities ())
      aEntities.add (SMPBusinessCardEntity.createFromGenericObject (aEntity));
    if (aBusinessCardMgr.createOrUpdateSMPBusinessCard (aServiceGroup, aEntities) == null)
    {
      if (LOGGER.isErrorEnabled ())
        LOGGER.error (LOG_PREFIX +
                      "Finished createBusinessCard(" +
                      sServiceGroupID +
                      "," +
                      aBusinessCard +
                      ") - failure");
      s_aStatsCounterError.increment ("createBusinessCard");
      return ESuccess.FAILURE;
    }

    if (LOGGER.isInfoEnabled ())
      LOGGER.info (LOG_PREFIX + "Finished createBusinessCard(" + sServiceGroupID + "," + aBusinessCard + ") - success");
    s_aStatsCounterSuccess.increment ("createBusinessCard");
    return ESuccess.SUCCESS;
  }

  /**
   * Delete an existing business card.
   *
   * @param sServiceGroupID
   *        The service group (participant) ID.
   * @param aCredentials
   *        The credentials to be used. May not be <code>null</code>.
   * @return {@link ESuccess}
   * @throws SMPServerException
   *         In case of error
   * @since 5.0.2
   */
  @Nonnull
  public ESuccess deleteBusinessCard (@Nonnull final String sServiceGroupID,
                                      @Nonnull final BasicAuthClientCredentials aCredentials) throws SMPServerException
  {
    if (LOGGER.isInfoEnabled ())
      LOGGER.info (LOG_PREFIX + "DELETE /businesscard/" + sServiceGroupID);
    s_aStatsCounterInvocation.increment ("deleteBusinessCard");

    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
    if (aServiceGroupID == null)
    {
      // Invalid identifier
      throw new SMPBadRequestException ("Failed to parse serviceGroup '" + sServiceGroupID + "'",
                                        m_aAPIProvider.getCurrentURI ());
    }

    final ISMPUserManager aUserMgr = SMPMetaManager.getUserMgr ();
    final ISMPUser aSMPUser = aUserMgr.validateUserCredentials (aCredentials);
    aUserMgr.verifyOwnership (aServiceGroupID, aSMPUser);

    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
    if (aBusinessCardMgr == null)
    {
      throw new SMPBadRequestException ("This SMP server does not support the BusinessCard API",
                                        m_aAPIProvider.getCurrentURI ());
    }
    final ISMPBusinessCard aBusinessCard = aBusinessCardMgr.getSMPBusinessCardOfID (sServiceGroupID);
    if (aBusinessCard == null)
    {
      // No such business card
      throw new SMPNotFoundException ("No BusinessCard assigned to serviceGroup '" + sServiceGroupID + "'",
                                      m_aAPIProvider.getCurrentURI ());
    }

    aBusinessCardMgr.deleteSMPBusinessCard (aBusinessCard);

    if (LOGGER.isInfoEnabled ())
      LOGGER.info (LOG_PREFIX + "Finished deleteBusinessCard(" + sServiceGroupID + ")");
    s_aStatsCounterSuccess.increment ("deleteBusinessCard");
    return ESuccess.SUCCESS;
  }

  /**
   * @return The statistics data with the invocation counter.
   */
  @Nonnull
  public static IStatisticsHandlerKeyedCounter getInvocationCounter ()
  {
    return s_aStatsCounterInvocation;
  }

  /**
   * @return The statistics data with the successful invocation counter.
   */
  @Nonnull
  public static IStatisticsHandlerKeyedCounter getSuccessCounter ()
  {
    return s_aStatsCounterSuccess;
  }

  /**
   * @return The statistics data with the error invocation counter.
   */
  @Nonnull
  public static IStatisticsHandlerKeyedCounter getErrorCounter ()
  {
    return s_aStatsCounterError;
  }
}
