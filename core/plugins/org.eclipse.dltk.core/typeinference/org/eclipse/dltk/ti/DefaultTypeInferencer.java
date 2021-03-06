/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *

 *******************************************************************************/
package org.eclipse.dltk.ti;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.ti.goals.AbstractTypeGoal;
import org.eclipse.dltk.ti.goals.FieldReferencesGoal;
import org.eclipse.dltk.ti.goals.FieldReferencesGoalEvaluator;
import org.eclipse.dltk.ti.goals.GoalEvaluator;
import org.eclipse.dltk.ti.goals.IGoal;
import org.eclipse.dltk.ti.goals.MethodCallsGoal;
import org.eclipse.dltk.ti.goals.MethodCallsGoalEvaluator;
import org.eclipse.dltk.ti.goals.NullGoalEvaluator;
import org.eclipse.dltk.ti.statistics.IEvaluationStatisticsRequestor;
import org.eclipse.dltk.ti.types.IEvaluatedType;

/**
 * <p>
 * Default DLTK type inferencing implementation, that uses ideas of
 * demand-driven analisys with a subgoal pruning (see GoalEngine class).Type
 * evaluation becomes a root goal for a GoalEngine.
 *
 * <p>
 * Cause this class is common, it doesn't provide lots of evaluators. Only
 * FieldReferencesGoalEvaluator and MethodCallsGoalEvaluator registered. Please,
 * look for their javadocs for more info.
 *
 * <p>
 * User can register evaluators via registerEvaluator() method. Also user are
 * able to provide custom evaluators factory, it will have higher priority, than
 * evaluators, registered via registerEvaluator() method.
 *
 */
public class DefaultTypeInferencer implements ITypeInferencer {

	private static class ProxyStatisticsRequestor implements
			IEvaluationStatisticsRequestor {

		public void evaluationStarted(IGoal rootGoal) {
			for (Iterator iterator = statRequestors.iterator(); iterator
					.hasNext();) {
				IEvaluationStatisticsRequestor t = (IEvaluationStatisticsRequestor) iterator
						.next();
				if (t != null) {
					t.evaluationStarted(rootGoal);
				}
			}
		}

		public void evaluatorInitialized(GoalEvaluator evaluator,
				IGoal[] subgoals, long time) {
			for (Iterator iterator = statRequestors.iterator(); iterator
					.hasNext();) {
				IEvaluationStatisticsRequestor t = (IEvaluationStatisticsRequestor) iterator
						.next();
				if (t != null) {
					t.evaluatorInitialized(evaluator, subgoals, time);
				}
			}
		}

		public void evaluatorProducedResult(GoalEvaluator evaluator,
				Object result, long time) {
			for (Iterator iterator = statRequestors.iterator(); iterator
					.hasNext();) {
				IEvaluationStatisticsRequestor t = (IEvaluationStatisticsRequestor) iterator
						.next();
				if (t != null) {
					t.evaluatorProducedResult(evaluator, result, time);
				}
			}

		}

		public void evaluatorReceivedResult(GoalEvaluator evaluator,
				IGoal finishedGoal, IGoal[] newSubgoals, long time) {
			for (Iterator iterator = statRequestors.iterator(); iterator
					.hasNext();) {
				IEvaluationStatisticsRequestor t = (IEvaluationStatisticsRequestor) iterator
						.next();
				if (t != null) {
					t.evaluatorReceivedResult(evaluator, finishedGoal,
							newSubgoals, time);
				}
			}
		}

		public void goalEvaluatorAssigned(IGoal goal, GoalEvaluator evaluator) {
			for (Iterator iterator = statRequestors.iterator(); iterator
					.hasNext();) {
				IEvaluationStatisticsRequestor t = (IEvaluationStatisticsRequestor) iterator
						.next();
				if (t != null) {
					t.goalEvaluatorAssigned(goal, evaluator);
				}
			}
		}

		public void goalStateChanged(IGoal goal, GoalState state,
				GoalState oldState) {
			for (Iterator iterator = statRequestors.iterator(); iterator
					.hasNext();) {
				IEvaluationStatisticsRequestor t = (IEvaluationStatisticsRequestor) iterator
						.next();
				if (t != null) {
					t.goalStateChanged(goal, state, oldState);
				}
			}
		}

	}

	private class MapBasedEvaluatorFactory implements IGoalEvaluatorFactory {

		public GoalEvaluator createEvaluator(IGoal goal) {
			Object evaluator = null;
			if (userFactory != null) {
				evaluator = userFactory.createEvaluator(goal);
				if (evaluator != null) {
					return (GoalEvaluator) evaluator;
				}
			}

			Class goalClass = goal.getClass();
			evaluator = evaluators.get(goalClass);
			if (evaluator == null || (!(evaluator instanceof Class))) {
				// throw new RuntimeException("No evaluator registered for "
				// + goalClass.getName() + " : " + goal);
				String className = goalClass.getName();
				if (DLTKCore.DEBUG) {
					System.err.println("No evaluator registered for " //$NON-NLS-1$
							+ className.substring(className.lastIndexOf('.'))
							+ ": " + goal + " - using NullGoalEvaluator"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				return new NullGoalEvaluator(goal);
			}
			Class evalClass = (Class) evaluator;
			GoalEvaluator newInstance;

			try {
				newInstance = (GoalEvaluator) evalClass.getConstructor(
						new Class[] { IGoal.class }).newInstance(
						new Object[] { goal });
				return newInstance;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}

			return null;
		}

	}

	private Map evaluators = new HashMap();

	private static Set statRequestors = new HashSet();

	private final GoalEngine engine;
	private final IGoalEvaluatorFactory userFactory;

	private final ProxyStatisticsRequestor stat = new ProxyStatisticsRequestor();

	private void initStdGoals() {
		registerEvaluator(FieldReferencesGoal.class,
				FieldReferencesGoalEvaluator.class);
		registerEvaluator(MethodCallsGoal.class, MethodCallsGoalEvaluator.class);
	}

	public DefaultTypeInferencer(IGoalEvaluatorFactory userFactory) {
		engine = new GoalEngine(new MapBasedEvaluatorFactory());
		this.userFactory = userFactory;
		initStdGoals();
	}

	public void registerEvaluator(Class goalClass, Class evaluatorClass) {
		Assert.isLegal((IGoal.class.isAssignableFrom(goalClass)));
		Assert.isLegal(GoalEvaluator.class.isAssignableFrom(evaluatorClass));
		evaluators.put(goalClass, evaluatorClass);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.dltk.ti.ITypeInferencer#evaluateType(org.eclipse.dltk.ti.AbstractTypeGoal,
	 *      long)
	 */
	public IEvaluatedType evaluateType(AbstractTypeGoal goal, int timeLimit) {
		Object result = this.evaluateType(goal, new TimelimitPruner(timeLimit));
		return (IEvaluatedType) result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.dltk.ti.ITypeInferencer#evaluateType(org.eclipse.dltk.ti.AbstractTypeGoal)
	 */
	public IEvaluatedType evaluateType(AbstractTypeGoal goal, IPruner pruner) {
		return (IEvaluatedType) engine.evaluateGoal(goal, pruner, stat);
	}

	protected Object evaluateGoal(IGoal goal, IPruner pruner) {
		return engine.evaluateGoal(goal, pruner, stat);
	}

	public IEvaluatedType evaluateType(AbstractTypeGoal goal) {
		return evaluateType(goal, null);
	}

	public static void addEvaluationStatisticsRequestor(
			IEvaluationStatisticsRequestor r) {
		statRequestors.add(r);
	}

	public static void removeEvaluationStatisticsRequestor(
			IEvaluationStatisticsRequestor r) {
		statRequestors.remove(r);
	}

}
