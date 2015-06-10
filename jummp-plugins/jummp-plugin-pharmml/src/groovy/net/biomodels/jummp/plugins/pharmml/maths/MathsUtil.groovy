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
*
* Additional permission under GNU Affero GPL version 3 section 7
*
* If you modify Jummp, or any covered work, by linking or combining it with
* LibPharmml (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of LibPharmml used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.plugins.pharmml.maths

import eu.ddmore.libpharmml.dom.maths.Binoperator
import eu.ddmore.libpharmml.dom.maths.EquationType
import eu.ddmore.libpharmml.dom.maths.Equation
import eu.ddmore.libpharmml.dom.maths.Unioperator

import javax.xml.bind.JAXBElement
import eu.ddmore.libpharmml.dom.maths.Binop
import eu.ddmore.libpharmml.dom.maths.Constant
import eu.ddmore.libpharmml.dom.maths.Uniop
import eu.ddmore.libpharmml.dom.maths.LogicUniOp
import eu.ddmore.libpharmml.dom.maths.LogicBinOp
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef
import eu.ddmore.libpharmml.dom.maths.FunctionCallType
import eu.ddmore.libpharmml.dom.maths.Piecewise
import eu.ddmore.libpharmml.dom.maths.Piece
import eu.ddmore.libpharmml.dom.maths.Condition
import eu.ddmore.libpharmml.dom.commontypes.TrueBoolean
import eu.ddmore.libpharmml.dom.commontypes.FalseBoolean

class MathsUtil {
	private static def pharmMap = [ "plus":"+",
        "minus":"-",
        "times":" &times;",
        "power":"^",
        "lt": "&lt;",
        "leq": "&le;",
        "gt":"&gt;",
        "geq":"&ge;",
        "eq":"=",
        "neq":"&ne;",
        "and":"&and;",
        "or":"&or;",
        "gammaln":"&Gamma;",
        "factln": "!",
        "isDefined": "&#x2061;",
        "not": "&#xac;"
    ]

	public static List<MathsSymbol> convertToSymbols(def equation) {
		List<MathsSymbol> symbols = new LinkedList<MathsSymbol>();
		convertJAX(symbols, equation)
		System.out.println("CONVERTED TO ${symbols}")
    	    	return symbols;
	}

	private static void convertJAX(List<MathsSymbol> symbols, def jaxObject) {
		if (jaxObject instanceof JAXBElement) { //shouldnt really happen, but sanity check.
            convertJAX(symbols, jaxObject.getValue())
            return
		}
		MathsSymbol symbol=getSymbol(jaxObject)
		if (symbol) {
			symbols.add(symbol)
		}
		List<JAXBElement> subtree=getSubTree(jaxObject, symbols)
		if (!subtree.isEmpty()) {
			subtree.each {
				convertJAX(symbols, it);
			}
		}
	}

	private static List getSubTree(def jaxObject, List<MathsSymbol> symbols) {
		List subTree = new LinkedList()
		if (jaxObject instanceof EquationType || jaxObject instanceof Equation) {
            if (jaxObject.getScalarOrSymbRefOrBinop()) {
                jaxObject.getScalarOrSymbRefOrBinop().each {
                    subTree.add(it.getValue())
                }
            } else {
                //assume this is 0.3
                addIfExists(jaxObject.uniop, subTree)
                addIfExists(jaxObject.binop, subTree)
                addIfExists(jaxObject.symbRef, subTree)
                addIfExists(jaxObject.scalar, subTree)
                addIfExists(jaxObject.functionCall, subTree)
                addIfExists(jaxObject.piecewise, subTree)
            }
        }
		else if (jaxObject instanceof LogicBinOp) {
			jaxObject.getContent().each {
				subTree.add(it.getValue())
			}
		} else if (jaxObject instanceof Binop) {
            subTree.add(jaxObject.operand1.toJAXBElement())
            subTree.add(jaxObject.operand2.toJAXBElement())
        }
		else if (jaxObject instanceof FunctionCallType) {
			List args=jaxObject.functionArgument
			args.each {
				addIfExists(it.equation, subTree)
				addIfExists(it.symbRef, subTree)
				addIfExists(it.scalar, subTree)
				addIfExists(it.constant, subTree)
			}
		}
		else if (jaxObject instanceof Piecewise) {
			List pieces=jaxObject.getPiece()
			subTree.addAll(pieces)
		}
		else if (jaxObject instanceof Piece) {
			addCommonSubElements(jaxObject, subTree)
			Condition condition=jaxObject.getCondition()
			if (!condition.getOtherwise()) {
				addIfExists(condition.getLogicBinop(),subTree)
				addIfExists(condition.getLogicUniop(),subTree)
				addIfExists(condition.getBoolean(),subTree)
			}

		}
		else if (jaxObject instanceof Uniop) {
			addCommonSubElements(jaxObject, subTree)
		}
		else if (jaxObject instanceof LogicUniOp) {
			addCommonSubElements(jaxObject, subTree)
			addIfExists(jaxObject.logicBinop, subTree)
			addIfExists(jaxObject.logicUniop, subTree)
		}
		return subTree
	}

	private static void addCommonSubElements(def jaxObject, List subTree) {
		addIfExists(jaxObject.binop, subTree)
		addIfExists(jaxObject.uniop, subTree)
		addIfExists(jaxObject.symbRef, subTree)
		addIfExists(jaxObject.functionCall, subTree)
		addIfExists(jaxObject.scalar, subTree)
		addIfExists(jaxObject.constant, subTree)
	}

	private static void addIfExists(Object object, List list) {
		if (object) {
			list.add(object)
		}
	}

	private static MathsSymbol getSymbol(SymbolRef jaxObject) {
		String varName="";
		/*if (jaxObject.getBlkIdRef()) {
			varName=jaxObject.getBlkIdRef()+":";
		}*/
		varName += jaxObject.getSymbIdRef();
		return new MathsSymbol(varName, varName)
	}

	private static MathsSymbol getSymbol(FunctionCallType jaxObject) {
		MathsSymbol tmp = getSymbol(jaxObject.symbRef)
		return new FunctionSymbol(tmp.mapsTo, tmp.mapsTo, jaxObject.functionArgument.size())
	}

	private static MathsSymbol getSymbol(Piecewise jaxObject) {
		return new PiecewiseSymbol(jaxObject.getPiece().size())
	}

	private static MathsSymbol getSymbol(Piece jaxObject) {
		Condition condition = jaxObject.getCondition()
		if (condition.getOtherwise()) {
			return new PieceSymbol(PieceSymbol.ConditionType.OTHERWISE)
		}
		return new PieceSymbol(PieceSymbol.ConditionType.EXTERNAL)
	}


	private static MathsSymbol getSymbol(TrueBoolean jaxObject) {
		return new MathsSymbol("true", "true");
	}

	private static MathsSymbol getSymbol(FalseBoolean jaxObject) {
		return new MathsSymbol("false", "false");
	}

    private static MathsSymbol getSymbol(Constant jaxObject) {
        String op = jaxObject.op
        switch(op) {
            case "pi":
                return new MathsSymbol(op, "&Pi;")
            case "notanumber":
                return new MathsSymbol(op, "NaN")
            case "exponentiale":
                return new MathsSymbol(op, "&ee;")
            case "infinity":
                return new MathsSymbol(op, "&infin;")
        }
    }

    private static MathsSymbol getSymbol(def jaxObject) {
        if (jaxObject instanceof Binop) {
            Binoperator o = jaxObject.operator
            switch (o) {
                case Binoperator.DIVIDE:
                    return OperatorSymbol.DivideSymbol()
                case Binoperator.MIN:
                case Binoperator.MAX:
                    return new OperatorSymbol(o.getOperator(), convertBinoperatorToSymbol(o),
                        OperatorSymbol.OperatorType.MINMAX)
                case Binoperator.ROOT:
                    return new OperatorSymbol(o.getOperator(), convertBinoperatorToSymbol(o),
                        OperatorSymbol.OperatorType.ROOT)
                case Binoperator.POWER:
                    return new OperatorSymbol(o.getOperator(), convertBinoperatorToSymbol(o),
                        OperatorSymbol.OperatorType.POWER)
                default:
                    return new OperatorSymbol(o.getOperator(), convertBinoperatorToSymbol(o),
                        OperatorSymbol.OperatorType.BINARY)
            }
        }
        if (jaxObject instanceof LogicBinOp) {
            final String op = jaxObject.op
            return new OperatorSymbol(op, convertTextToSymbol(op),
                    OperatorSymbol.OperatorType.BINARY)
        }
        if (jaxObject instanceof LogicUniOp) {
            return new OperatorSymbol(jaxObject.op, jaxObject.op,
                    OperatorSymbol.OperatorType.UNARY)
        }
        if (jaxObject instanceof Uniop || jaxObject instanceof LogicUniOp) {
            Unioperator o = jaxObject.operator
            switch (o) {
                case Unioperator.GAMMALN:
                    return new OperatorSymbol(o.getOperator(), convertUnioperatorToSymbol(o),
                        OperatorSymbol.OperatorType.GAMMALN)
                case  Unioperator.FACTLN:
                    return new OperatorSymbol(o.getOperator(), convertUnioperatorToSymbol(o),
                        OperatorSymbol.OperatorType.FACTLN)
                    case Unioperator.SQRT:
                        return new OperatorSymbol(o.getOperator(), convertUnioperatorToSymbol(o),
                            OperatorSymbol.OperatorType.SQROOT)
                    default:
                        OperatorSymbol op = new OperatorSymbol(o.getOperator(),
                            convertUnioperatorToSymbol(o), OperatorSymbol.OperatorType.UNARY)
                        if (o == Unioperator.MINUS) {
                            op.omitBraces = true
                        }
                        return op
            }
        }
        if (jaxObject.getClass().getCanonicalName().contains("eu.ddmore.libpharmml.dom.commontypes")) {
            String objectClass = "${jaxObject.getValue()}"
            return new MathsSymbol(objectClass, objectClass)
        }
        return null
    }

    private static String convertBinoperatorToSymbol(Binoperator b) {
        return convertTextToSymbol(b.getOperator())
    }

    private static String convertUnioperatorToSymbol(Unioperator u) {
        return convertTextToSymbol(u.getOperator())
    }

    private static String convertTextToSymbol(String pharmText) {
        if (pharmMap.containsKey(pharmText)) {
            return pharmMap.get(pharmText)
        }
        return pharmText
    }
}
