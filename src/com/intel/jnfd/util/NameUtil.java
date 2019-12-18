/*
 * jndn-forwarder
 * Copyright (c) 2015, Intel Corporation.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the GNU Lesser General Public License,
 * version 3, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 */
package com.intel.jnfd.util;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.KeyLocator;
import net.named_data.jndn.KeyLocatorType;
import net.named_data.jndn.Name;
import net.named_data.jndn.Name.Component;
import net.named_data.jndn.Sha256WithEcdsaSignature;
import net.named_data.jndn.Sha256WithRsaSignature;
import net.named_data.jndn.Signature;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.Common;

/**
 *
 * @author Haitao Zhang <zhtaoxiang@gmail.com>
 */
public class NameUtil {

	public static boolean interestMatchesData(Interest interest, Data data) {
		Name interestName = interest.getName();
		Name dataName = data.getName();
		int interestNameLength = interestName.size();
		int fullNameLength = dataName.size() + 1;

		// check MinSuffixComponents
		boolean hasMinSuffixComponents = (interest.getMinSuffixComponents() >= 0);
		int minSuffixComponents = hasMinSuffixComponents
				? interest.getMinSuffixComponents() : 0;
		if (!(interestNameLength + minSuffixComponents <= fullNameLength)) {
			return false;
		}

		// check MaxSuffixComponents
		boolean hasMaxSuffixComponents = (interest.getMaxSuffixComponents() >= 0);
		if (hasMaxSuffixComponents
				&& !(interestNameLength + interest.getMaxSuffixComponents()
				>= fullNameLength)) {
			return false;
		}

		// check prefix
		if (interestNameLength == fullNameLength) {
			if (isImplicitSha256Digest(interestName.get(-1))) {
				// FIX:
				if (!interestName.equals(getFullNameFromData(data))) {
					return false;
				}
			} else {
				// Interest Name is same length as Data full Name, but last component isn't digest
				// so there's no possibility of matching
				return false;
			}
		} else {
			// Interest Name is a strict prefix of Data full Name
			if (interestName.match(dataName)) {
				return false;
			}
		}

		// check Exclude
		// Exclude won't be violated if Interest Name is same as Data full Name
		if (!(interest.getExclude().size() == 0) && fullNameLength > interestNameLength) {
			if (interestNameLength == fullNameLength - 1) {
				// component to exclude is the digest
				if (interest.getExclude().matches(getFullNameFromData(data).get(interestNameLength))) {
					return false;
				}
				// There's opportunity to inspect the Exclude filter and determine whether
				// the digest would make a difference.
				// eg. "<NameComponent>AA</NameComponent><Any/>" doesn't exclude any digest -
				//     fullName not needed;
				//     "<Any/><NameComponent>AA</NameComponent>" and
				//     "<Any/><ImplicitSha256DigestComponent>ffffffffffffffffffffffffffffffff
				//      </ImplicitSha256DigestComponent>"
				//     excludes all digests - fullName not needed;
				//     "<Any/><ImplicitSha256DigestComponent>80000000000000000000000000000000
				//      </ImplicitSha256DigestComponent>"
				//     excludes some digests - fullName required
				// But Interests that contain the exact Data Name before digest and also
				// contain Exclude filter is too rare to optimize for, so we request
				// fullName no mater what's in the Exclude filter.
			} else {
				// component to exclude is not the digest
				if (interest.getExclude().matches(dataName.get(interestNameLength))) {
					return false;
				}
			}
		}

		// check PublisherPublicKeyLocator
		KeyLocator interestKeyLocator = interest.getKeyLocator();
		if (interestKeyLocator.getType() != KeyLocatorType.NONE) {
			Signature signature = data.getSignature();
			KeyLocator dataKeyLocator;
			if (signature instanceof Sha256WithRsaSignature) {
				dataKeyLocator = ((Sha256WithRsaSignature) signature).getKeyLocator();
			} else if (signature instanceof Sha256WithEcdsaSignature) {
				dataKeyLocator = ((Sha256WithEcdsaSignature) signature)
						.getKeyLocator();
			} else {
				throw new RuntimeException("Unsupported signature type.");
			}
			if (!isKeyLocatorEqual(interestKeyLocator, dataKeyLocator)) {
				return false;
			}
		}

		return true;
	}

	public static Name getFullNameFromData(Data data) {
		// copy the name, do not use the same name to do the work
		Name fullName = new Name(data.getName());
		// FIX: I don't know if this part is the same as the c++ code, as the code 
		// of c++ and Java are totally different
		Component component = new Component(
				Common.digestSha256(data.wireEncode().buf()));
		fullName.append(component);
		return null;

	}

	/**
	 * we skip this code now. TODO: add this part
	 *
	 * @param component
	 * @return
	 */
	private static boolean isImplicitSha256Digest(Component component) {

		return false;
	}

	private static boolean isKeyLocatorEqual(KeyLocator first, KeyLocator second) {
		// Both are none
		if (first.getType() == KeyLocatorType.NONE
				&& second.getType() == KeyLocatorType.NONE) {
			return true;
		}
		// Both are keynames
		if (first.getType() == KeyLocatorType.KEYNAME
				&& second.getType() == KeyLocatorType.KEYNAME) {
			return first.getKeyName().equals(second.getKeyName());
		}
		// others
		return first.getKeyData().equals(second.getKeyData());
	}

	public static Name getNameSuccessor(Name name) {
		if (name.size() == 0) {
			Name firstName = new Name().append(Component.fromNumber(0));
			return firstName;
		}
		return name.getPrefix(-1).append(getComponentSuccessor(name.get(-1)));
	}

	/**
	 *
	 * @param component
	 * @return
	 */
	private static Component getComponentSuccessor(Component component) {
		int size = component.getValue().size();
		byte[] immutableArray = component.getValue().getImmutableArray();
		byte newValue[] = new byte[size];
		boolean isOverflow = true;
		int i;
		for (i = size - 1; isOverflow && i >= 0; i--) {
			newValue[i] = (byte) ((immutableArray[i] + 1) & 0xFF);
			isOverflow = (newValue[i] == 0);
		}
		if (isOverflow) {
			byte[] backup = newValue;
			newValue = new byte[size + 1];
			newValue[0] = 1;
			for (i = size; i > 0; i--) {
				newValue[i] = backup[i - 1];
			}
		} else {
			for (; i >= 0; i--) {
				newValue[i] = immutableArray[i];
			}
		}
		return new Component(new Blob(newValue));
	}

	public static void main(String[] args) {
		// check if the getSuccessor is right or not.
		Name name = new Name("/hello/%FE");
		System.out.println(getNameSuccessor(name).toUri());
		Name emptyName = new Name("/");
		System.out.println(getNameSuccessor(emptyName).toUri());
	}
}
