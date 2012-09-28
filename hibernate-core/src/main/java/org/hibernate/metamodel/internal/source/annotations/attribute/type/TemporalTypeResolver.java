/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.metamodel.internal.source.annotations.attribute.type;

import java.sql.Time;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.TemporalType;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.internal.source.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.type.StandardBasicTypes;
import org.jboss.jandex.AnnotationInstance;

/**
 * @author Strong Liu
 * @author Brett Meyer
 */
public class TemporalTypeResolver extends AbstractAttributeTypeResolver {
	private final BasicAttribute mappedAttribute;
	private final boolean isMapKey;
	public TemporalTypeResolver(BasicAttribute mappedAttribute) {
		if ( mappedAttribute == null ) {
			throw new AssertionFailure( "MappedAttribute is null" );
		}
		this.mappedAttribute = mappedAttribute;
		this.isMapKey = false;//todo
	}

	@Override
	public String resolveHibernateTypeName(AnnotationInstance temporalAnnotation) {
		Class attributeType = mappedAttribute.getAttributeType();
		
		if ( isTemporalType( attributeType ) ) {
			if ( mappedAttribute.isVersioned() && mappedAttribute.getVersionSourceType() != null ) {
				return mappedAttribute.getVersionSourceType().typeName();
			}
			if ( temporalAnnotation == null ) {
				// Although JPA 2.1 states that @Temporal is required on
				// Date/Calendar attributes, allow it to be left off in order
				// to support legacy mappings.
				// java.util.Date -> TimestampType
				// java.sql.Timestamp -> TimestampType
				// java.sql.Date -> DateType
				// java.sql.Time -> TimeType
				// java.util.Calendar -> CalendarType
				if ( java.sql.Date.class.isAssignableFrom( attributeType ) ) {
					return StandardBasicTypes.DATE.getName();
				} else if ( Time.class.isAssignableFrom( attributeType ) ) {
					return StandardBasicTypes.TIME.getName();
				} else if ( Calendar.class.isAssignableFrom( attributeType ) ) {
					return StandardBasicTypes.CALENDAR.getName();
				} else {
					return StandardBasicTypes.TIMESTAMP.getName();
				}
			} else {
				final TemporalType temporalType = JandexHelper.getEnumValue( temporalAnnotation, "value", TemporalType.class );
				final boolean isDate = Date.class.isAssignableFrom( attributeType );
				String type;
				switch ( temporalType ) {
					case DATE:
						type = isDate ? StandardBasicTypes.DATE.getName() : StandardBasicTypes.CALENDAR_DATE.getName();
						break;
					case TIME:
						type = StandardBasicTypes.TIME.getName();
						if ( !isDate ) {
							throw new NotYetImplementedException( "Calendar cannot persist TIME only" );
						}
						break;
					case TIMESTAMP:
						type = isDate ? StandardBasicTypes.TIMESTAMP.getName() : StandardBasicTypes.CALENDAR.getName();
						break;
					default:
						throw new AssertionFailure( "Unknown temporal type: " + temporalType );
				}
				return type;
			}
		} else {
			if ( temporalAnnotation != null ) {
				throw new AnnotationException(
						"@Temporal should only be set on a java.util.Date or java.util.Calendar property: " + mappedAttribute
								.getName()
				);
			}
		}
		return null;
	}

	@Override
	protected AnnotationInstance getTypeDeterminingAnnotationInstance() {
		return JandexHelper.getSingleAnnotation(
				mappedAttribute.annotations(),
				JPADotNames.TEMPORAL
		);
	}

	private static boolean isTemporalType(Class type) {
		return Date.class.isAssignableFrom( type ) || Calendar.class.isAssignableFrom( type );
	}
}
