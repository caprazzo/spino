/**
 * Copyright 2013 Matteo Caprari
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spino.core;

import com.hazelcast.core.Member;

import java.io.Serializable;
import java.net.URL;

/**
 * Binds an ServiceLocation to a Member.
 */
final class LocationBinding implements Serializable {

    private final Member member;
    private final ServiceLocation serviceInstance;

    /**
     * Creates a new instance of LocationBinding
     * @param serviceInstance - the serviceInstance
     * @param member - the cluster member that published information about the serviceInstance
     */
    LocationBinding(ServiceLocation serviceInstance, Member member) {
        this.member = member;
        this.serviceInstance = serviceInstance;
    }

    /**
     * Creates a new instance of LocationBinding
     * @param service - name of the serviceInstance
     * @param address - URL of the serviceInstance
     * @param member - the cluster member that published information about the serviceInstance
     */
    LocationBinding(String service, URL address, Member member) {
        this(new ServiceLocation(service, address), member);
    }

    URL getAddress() {
        return serviceInstance.getAddress();
    }

    Member getMember() {
        return member;
    }

    String getService() {
        return serviceInstance.getService();
    }

    ServiceLocation getServiceInstance() {
        return serviceInstance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocationBinding other = (LocationBinding) o;

        if (!serviceInstance.equals(other.serviceInstance)) return false;
        if (!member.equals(other.member)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = member.hashCode();
        result = 31 * result + serviceInstance.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("LocationBinding(member=%s, serviceInstance=%s)", member, serviceInstance);
    }
}
