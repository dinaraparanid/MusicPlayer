package com.dinaraparanid.prima.core

import java.io.Serializable

/** Contact entity for [android.provider.ContactsContract.Contacts] */

class Contact(val id: Long, val customRingtone: String, val displayName: String) : Serializable