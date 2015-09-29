package com.helger.peppol.smpserver.data.xml;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.peppol.smpserver.data.IDataUser;
import com.helger.photon.basic.security.user.IUser;

public class XMLDataUser implements IDataUser
{
  private final IUser m_aUser;

  public XMLDataUser (@Nonnull final IUser aUser)
  {
    m_aUser = ValueEnforcer.notNull (aUser, "User");
  }

  @Nonnull
  public IUser getUser ()
  {
    return m_aUser;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_aUser.getID ();
  }

  @Nonnull
  @Nonempty
  public String getUserName ()
  {
    return m_aUser.getLoginName ();
  }
}