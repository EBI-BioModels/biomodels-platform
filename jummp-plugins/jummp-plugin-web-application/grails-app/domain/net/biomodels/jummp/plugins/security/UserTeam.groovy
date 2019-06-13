/**
 * Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
 * Deutsches Krebsforschungszentrum (DKFZ)
 *
 * This file is part of Jummp.
 *
 * Jummp is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 **/

package net.biomodels.jummp.plugins.security

class UserTeam implements Serializable {
    private static final long serialVersionUID = 1L
    User user
    Team team

    @Override
    boolean equals(Object other) {
        if (!(other instanceof UserTeam)) {
            return false
        }
        return user?.id == other.user?.id && team?.id == other.team?.id
    }

    @Override
    int hashCode() {
        int result = 17
        if (user) {
            result = 31 * result + user.id
        }
        if (team) {
            result = 31 * result + team.id
        }
        return result
    }

    static UserTeam get(long userId, long teamId) {
        return find("from UserTeam where user.id=:uId and team.id=:tId", [uId: userId, tId: teamId])
    }

    static UserTeam create(User user, Team team, boolean flush = false) {
        return new UserTeam(user: user, team: team).save(flush: flush)
    }

    static boolean remove(User user, Team team, boolean flush = false) {
        UserTeam instance = UserTeam.findByUserAndTeam(user, team)
        return instance ? instance.delete(flush: flush) : false
    }

    static void removeAll(User user) {
        executeUpdate('DELETE FROM UserTeam WHERE user=:user', [user: user])
    }

    static void removeAll(Team team) {
        executeUpdate('DELETE FROM UserTeam WHERE team=:team', [team: team])
    }

    static mapping = {
        id composite: ['team', 'user']
        version false
    }
}
