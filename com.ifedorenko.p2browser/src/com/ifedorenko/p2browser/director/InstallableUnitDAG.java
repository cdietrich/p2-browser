/*******************************************************************************
 * Copyright (c) 2011 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package com.ifedorenko.p2browser.director;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.internal.p2.director.QueryableArray;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;

import com.ifedorenko.p2browser.model.match.IInstallableUnitMatcher;

@SuppressWarnings( "restriction" )
public class InstallableUnitDAG
{
    private final IInstallableUnit[] rootIUs;

    private final Map<IInstallableUnit, InstallableUnitInfo> units;

    public InstallableUnitDAG( IInstallableUnit[] rootIUs, Map<IInstallableUnit, InstallableUnitInfo> units )
    {
        this.rootIUs = rootIUs;
        this.units = Collections.unmodifiableMap( new LinkedHashMap<IInstallableUnit, InstallableUnitInfo>( units ) );
    }

    public Collection<InstallableUnitInfo> getRootInstallableUnits()
    {
        List<InstallableUnitInfo> result = new ArrayList<InstallableUnitInfo>();

        for ( IInstallableUnit root : rootIUs )
        {
            result.add( units.get( root ) );
        }

        return result;
    }

    public IQueryable<IInstallableUnit> toQueryable()
    {
        Set<IInstallableUnit> set = units.keySet();
        return new QueryableArray( set.toArray( new IInstallableUnit[set.size()] ) );
    }

    public InstallableUnitDAG filter( IInstallableUnitMatcher matcher )
    {
        Map<IInstallableUnit, InstallableUnitInfo> filtered =
            new LinkedHashMap<IInstallableUnit, InstallableUnitInfo>();

        for ( Map.Entry<IInstallableUnit, InstallableUnitInfo> entry : units.entrySet() )
        {
            if ( matcher.match( entry.getKey() ) )
            {
                filtered.put( entry.getKey(), new InstallableUnitInfo( entry.getValue().getInstallableUnit() ) );
            }
        }

        for ( InstallableUnitInfo unit : filtered.values() )
        {
            InstallableUnitInfo original = units.get( unit.getInstallableUnit() );

            for ( InstallableUnitInfo originalParent : original.getParents() )
            {
                InstallableUnitInfo parent = filtered.get( originalParent.getInstallableUnit() );
                if ( parent != null )
                {
                    unit.addParent( parent );
                }
            }

            for ( InstallableUnitInfo originalChild : original.getChildren() )
            {
                InstallableUnitInfo child = filtered.get( originalChild.getInstallableUnit() );
                if ( child != null )
                {
                    unit.addChild( child );
                }
            }
        }

        return new InstallableUnitDAG( rootIUs, filtered );
    }

    public InstallableUnitDAG sort( final Comparator<IInstallableUnit> comparator )
    {
        IInstallableUnit[] rootIUs = new IInstallableUnit[this.rootIUs.length];
        System.arraycopy( this.rootIUs, 0, rootIUs, 0, this.rootIUs.length );
        Arrays.sort( rootIUs, comparator );

        Map<IInstallableUnit, InstallableUnitInfo> sorted = new LinkedHashMap<IInstallableUnit, InstallableUnitInfo>();

        for ( Map.Entry<IInstallableUnit, InstallableUnitInfo> entry : this.units.entrySet() )
        {
            InstallableUnitInfo info = new InstallableUnitInfo( entry.getKey() );

            List<InstallableUnitInfo> children = new ArrayList<InstallableUnitInfo>( entry.getValue().getChildren() );
            Collections.sort( children, new Comparator<InstallableUnitInfo>()
            {
                @Override
                public int compare( InstallableUnitInfo a, InstallableUnitInfo b )
                {
                    return comparator.compare( a.getInstallableUnit(), b.getInstallableUnit() );
                }
            } );

            for ( InstallableUnitInfo child : children )
            {
                info.addChild( child );
            }

            for ( InstallableUnitInfo parent : entry.getValue().getParents() )
            {
                info.addParent( parent );
            }

            sorted.put( info.getInstallableUnit(), info );
        }

        return new InstallableUnitDAG( rootIUs, sorted );
    }

    public InstallableUnitInfo getInstallableUnit( IInstallableUnit unit )
    {
        return units.get( unit );
    }

    public Collection<InstallableUnitInfo> getInstallableUnits()
    {
        return units.values();
    }
}