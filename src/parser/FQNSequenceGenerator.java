package parser;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.jdt.core.dom.*;

public class FQNSequenceGenerator extends ASTVisitor {
	private static final String SEPARATOR = "#";
	
	private String className, superClassName;
	private int numOfExpressions = 0, numOfResolvedExpressions = 0;
	private StringBuilder fullTokens = new StringBuilder(), partialTokens = new StringBuilder();
	private StringBuilder holeTokens = new StringBuilder(), fqnTokens = new StringBuilder();
	private String fullSequence = null, partialSequence = null;
	private String holeSequence = null, fqnSequence = null;
	private String[] fullSequenceTokens, partialSequenceTokens;
	private String[] holeSequenceTokens, fqnSequenceTokens;
	
	public FQNSequenceGenerator(String className, String superClassName) {
		super(false);
		this.className = className;
		this.superClassName = superClassName;
	}

	public String[] getFullSequenceTokens() {
		if (fullSequenceTokens == null)
			buildFullSequence();
		return fullSequenceTokens;
	}

	public String[] getPartialSequenceTokens() {
		if (partialSequenceTokens == null)
			buildPartialSequence();
		return partialSequenceTokens;
	}

	public String[] getHoleSequenceTokens() {
		if (holeSequenceTokens == null)
			buildHoleSequence();
		return holeSequenceTokens;
	}

	public String[] getFQNSequenceTokens() {
		if (fqnSequenceTokens == null)
			buildFQNSequence();
		return fqnSequenceTokens;
	}

	public String getFullSequence() {
		if (fullSequence == null)
			buildFullSequence();
		return fullSequence;
	}

	public String getPartialSequence() {
		if (partialSequence == null)
			buildPartialSequence();
		return partialSequence;
	}

	public String getHoleSequence() {
		if (holeSequence == null)
			buildHoleSequence();
		return holeSequence;
	}

	public String getFQNSequence() {
		if (fqnSequence == null)
			buildFQNSequence();
		return fqnSequence;
	}

	private void buildFullSequence() {
		ArrayList<String> parts = buildSequence(fullTokens);
		this.fullSequence = parts.get(0);
		this.fullSequenceTokens = new String[parts.size() - 1];
		for (int i = 1; i < parts.size(); i++)
			this.fullSequenceTokens[i-1] = parts.get(i);
	}

	private void buildPartialSequence() {
		ArrayList<String> parts = buildSequence(partialTokens);
		this.partialSequence = parts.get(0);
		this.partialSequenceTokens = new String[parts.size() - 1];
		for (int i = 1; i < parts.size(); i++)
			this.partialSequenceTokens[i-1] = parts.get(i);
	}

	private void buildHoleSequence() {
		ArrayList<String> parts = buildSequence(holeTokens);
		this.holeSequence = parts.get(0);
		this.holeSequenceTokens = new String[parts.size() - 1];
		for (int i = 1; i < parts.size(); i++)
			this.holeSequenceTokens[i-1] = parts.get(i);
	}

	private void buildFQNSequence() {
		ArrayList<String> parts = buildSequence(fqnTokens);
		this.fqnSequence = parts.get(0);
		this.fqnSequenceTokens = new String[parts.size() - 1];
		for (int i = 1; i < parts.size(); i++)
			this.fqnSequenceTokens[i-1] = parts.get(i);
	}
	
	private ArrayList<String> buildSequence(StringBuilder tokens) {
		tokens.append(" ");
		ArrayList<String> l = new ArrayList<>();
		StringBuilder sequence = new StringBuilder(), token = null;
		for (int i = 0; i < tokens.length(); i++) {
			char ch = tokens.charAt(i);
			if (ch == ' ') {
				if (token != null) {
					String t = token.toString();
					l.add(t);
					sequence.append(t + " ");
					token = null;
				}
			} else {
				if (token == null)
					token = new StringBuilder();
				token.append(ch);
			}
		}
		l.add(0, sequence.toString());
		return l;
	}

	public int getNumOfExpressions() {
		return numOfExpressions;
	}

	public int getNumOfResolvedExpressions() {
		return numOfResolvedExpressions;
	}

	private Type getType(VariableDeclarationFragment node) {
		ASTNode p = node.getParent();
		if (p instanceof VariableDeclarationExpression)
			return ((VariableDeclarationExpression) p).getType();
		if (p instanceof VariableDeclarationStatement)
			return ((VariableDeclarationStatement) p).getType();
		return null;
	}

	private String getSignature(IMethodBinding method) {
		StringBuilder sb = new StringBuilder();
		sb.append(method.getDeclaringClass().getTypeDeclaration().getQualifiedName());
		sb.append("." + method.getName());
		sb.append("(");
		sb.append(SEPARATOR);
		for (ITypeBinding tb : method.getParameterTypes())
			sb.append(tb.getTypeDeclaration().getName() + "#");
		sb.append(")");
		return sb.toString();
	}

	static String getUnresolvedType(Type type) {
		if (type.isArrayType()) {
			ArrayType t = (ArrayType) type;
			return getUnresolvedType(t.getElementType()) + getDimensions(t.getDimensions());
		} else if (type.isIntersectionType()) {
			IntersectionType it = (IntersectionType) type;
			@SuppressWarnings("unchecked")
			ArrayList<Type> types = new ArrayList<>(it.types());
			String s = getUnresolvedType(types.get(0));
			for (int i = 1; i < types.size(); i++)
				s += " & " + getUnresolvedType(types.get(i));
			return s;
		} else if (type.isParameterizedType()) {
			ParameterizedType t = (ParameterizedType) type;
			return getUnresolvedType(t.getType());
		} else if (type.isUnionType()) {
			UnionType it = (UnionType) type;
			@SuppressWarnings("unchecked")
			ArrayList<Type> types = new ArrayList<>(it.types());
			String s = getUnresolvedType(types.get(0));
			for (int i = 1; i < types.size(); i++)
				s += " | " + getUnresolvedType(types.get(i));
			return s;
		} else if (type.isNameQualifiedType()) {
			NameQualifiedType qt = (NameQualifiedType) type;
			return qt.getQualifier().getFullyQualifiedName() + "." + qt.getName().getIdentifier();
		} else if (type.isPrimitiveType()) {
			return type.toString();
		} else if (type.isQualifiedType()) {
			QualifiedType qt = (QualifiedType) type;
			return getUnresolvedType(qt.getQualifier()) + "." + qt.getName().getIdentifier();
		} else if (type.isSimpleType()) {
			return type.toString();
		} else if (type.isWildcardType()) {
			WildcardType wt = (WildcardType) type;
			String s = "?";
			if (wt.getBound() != null) {
				if (wt.isUpperBound())
					s += "extends ";
				else
					s += "super ";
				s += getUnresolvedType(wt.getBound());
			}
			return s;
		}
		
		return null;
	}

	private static String getDimensions(int dimensions) {
		String s = "";
		for (int i = 0; i < dimensions; i++)
			s += "[]";
		return s;
	}

	static String getResolvedType(Type type) {
		ITypeBinding tb = type.resolveBinding();
		if (tb == null || tb.isRecovered())
			return getUnresolvedType(type);
		tb = tb.getTypeDeclaration();
		if (tb.isLocal() || tb.getQualifiedName().isEmpty())
			return getUnresolvedType(type);
		if (type.isArrayType()) {
			ArrayType t = (ArrayType) type;
			return getResolvedType(t.getElementType()) + getDimensions(t.getDimensions());
		} else if (type.isIntersectionType()) {
			IntersectionType it = (IntersectionType) type;
			@SuppressWarnings("unchecked")
			ArrayList<Type> types = new ArrayList<>(it.types());
			String s = getResolvedType(types.get(0));
			for (int i = 1; i < types.size(); i++)
				s += " & " + getResolvedType(types.get(i));
			return s;
		} else if (type.isParameterizedType()) {
			ParameterizedType t = (ParameterizedType) type;
			return getResolvedType(t.getType());
		} else if (type.isUnionType()) {
			UnionType it = (UnionType) type;
			@SuppressWarnings("unchecked")
			ArrayList<Type> types = new ArrayList<>(it.types());
			String s = getResolvedType(types.get(0));
			for (int i = 1; i < types.size(); i++)
				s += " | " + getResolvedType(types.get(i));
			return s;
		} else if (type.isNameQualifiedType()) {
			return tb.getQualifiedName();
		} else if (type.isPrimitiveType()) {
			return type.toString();
		} else if (type.isQualifiedType()) {
			return tb.getQualifiedName();
		} else if (type.isSimpleType()) {
			return tb.getQualifiedName();
		} else if (type.isWildcardType()) {
			WildcardType wt = (WildcardType) type;
			String s = "?";
			if (wt.getBound() != null) {
				if (wt.isUpperBound())
					s += "extends ";
				else
					s += "super ";
				s += getResolvedType(wt.getBound());
			}
			return s;
		}
		
		return null;
	}

	@Override
	public void preVisit(ASTNode node) {
		if (node instanceof Expression) {
			numOfExpressions++;
			Expression e = (Expression) node;
			if (e.resolveTypeBinding() != null && !e.resolveTypeBinding().isRecovered())
				numOfResolvedExpressions++;
		} else if (node instanceof Statement) {
			if (node instanceof ConstructorInvocation) {
				numOfExpressions++;
				if (((ConstructorInvocation) node).resolveConstructorBinding() != null && !((ConstructorInvocation) node).resolveConstructorBinding().isRecovered())
					numOfResolvedExpressions++;
			} else if (node instanceof SuperConstructorInvocation) {
				numOfExpressions++;
				if (((SuperConstructorInvocation) node).resolveConstructorBinding() != null && !((SuperConstructorInvocation) node).resolveConstructorBinding().isRecovered())
					numOfResolvedExpressions++;
			}
		} else if (node instanceof Type) {
			numOfExpressions++;
			Type t = (Type) node;
			if (t.resolveBinding() != null && !t.resolveBinding().isRecovered())
				numOfResolvedExpressions++;
		}
	}

	@Override
	public boolean visit(ArrayAccess node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(ArrayCreation node) {
		String utype = getUnresolvedType(node.getType()), rtype = getResolvedType(node.getType());
		this.partialTokens.append(" new " + utype + " ");
		this.holeTokens.append(" new <blank>." + utype + " ");
		this.fullTokens.append(" new " + rtype + " ");
		this.fqnTokens.append("new " + rtype + " ");
		if (node.getInitializer() != null)
			node.getInitializer().accept(this);
		else
			for (int i = 0; i < node.dimensions().size(); i++)
				((Expression) (node.dimensions().get(i))).accept(this);
		return false;
	}

	@Override
	public boolean visit(ArrayInitializer node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(AssertStatement node) {
		this.fullTokens.append(" assert ");
		this.partialTokens.append(" assert ");
		this.holeTokens.append(" assert ");
		this.fqnTokens.append(" assert ");
		return super.visit(node);
	}

	@Override
	public boolean visit(Assignment node) {
		node.getLeftHandSide().accept(this);
		this.fullTokens.append(" = ");
		this.holeTokens.append(" = ");
		this.partialTokens.append(" = ");
		this.fqnTokens.append(" = ");
		node.getRightHandSide().accept(this);
		return false;
	}

	@Override
	public boolean visit(Block node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(BooleanLiteral node) {
		this.fullTokens.append(" boolean ");
		this.fqnTokens.append(" boolean ");
		this.partialTokens.append(" boolean ");
		this.holeTokens.append(" boolean ");
		return false;
	}

	@Override
	public boolean visit(BreakStatement node) {
		return false;
	}

	@Override
	public boolean visit(CastExpression node) {
		String utype = getUnresolvedType(node.getType()), rtype = getResolvedType(node.getType());
		this.fullTokens.append(" " + rtype + " <cast> ");
		this.fqnTokens.append(" " + rtype + " <cast> ");
		this.partialTokens.append(" " + utype + " <cast> ");
		this.holeTokens.append(" <blank>." + utype + " <cast> " );
		node.getExpression().accept(this);
		return false;
	}

	@Override
	public boolean visit(CatchClause node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(CharacterLiteral node) {
		this.fullTokens.append(" char ");
		this.partialTokens.append(" char ");
		this.holeTokens.append(" char ");
		this.fqnTokens.append(" char ");
		return false;
	}

	@Override
	public boolean visit(ClassInstanceCreation node) {
		ITypeBinding tb = node.getType().resolveBinding();
		if (tb != null && tb.getTypeDeclaration().isLocal())
			return false;
		String utype = getUnresolvedType(node.getType());
		IMethodBinding b = node.resolveConstructorBinding();
		if (b == null) {
			this.fullTokens.append(" new " + utype + "(" + node.arguments().size() + ") ");
			this.fqnTokens.append(" new " + utype + "(" + node.arguments().size() + ") ");
		} else {
			this.fullTokens.append(" new " + getSignature(b.getMethodDeclaration()) + " ");
			this.fqnTokens.append(" new " + getSignature(b.getMethodDeclaration()) + " ");
		}
		this.partialTokens.append(" new " + utype + "(" + node.arguments().size() + ") ");
		this.holeTokens.append(" new <blank>." + utype + "(" + node.arguments().size() + ") ");
		for (Iterator it = node.arguments().iterator(); it.hasNext(); ) {
			Expression e = (Expression) it.next();
			e.accept(this);
		}
		if (node.getAnonymousClassDeclaration() != null)
			node.getAnonymousClassDeclaration().accept(this);
		return false;
	}

	@Override
	public boolean visit(ConditionalExpression node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(ConstructorInvocation node) {
		IMethodBinding b = node.resolveConstructorBinding();
		ITypeBinding tb = null;
		if (b != null && b.getDeclaringClass() != null)
			tb = b.getDeclaringClass().getTypeDeclaration();
		if (tb != null) {
			if (tb.isLocal() || tb.getQualifiedName().isEmpty())
				return false;
		}
		String name = "." + className + "(" + node.arguments().size() + ")";
		this.partialTokens.append(" " + name + " ");
		this.holeTokens.append(" <blank>." + name + " ");
		if (tb != null)
			name = getSignature(b.getMethodDeclaration());
		this.fullTokens.append(" " + name + " ");
		this.fqnTokens.append(" " + name + " ");
		for (int i = 0; i < node.arguments().size(); i++)
			((ASTNode) node.arguments().get(i)).accept(this);
		return false;
	}

	@Override
	public boolean visit(ContinueStatement node) {
		return false;
	}

	@Override
	public boolean visit(CreationReference node) {
		return false;
	}

	@Override
	public boolean visit(Dimension node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(DoStatement node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(EmptyStatement node) {
		return false;
	}

	@Override
	public boolean visit(EnhancedForStatement node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(EnumConstantDeclaration node) {
		return false;
	}

	@Override
	public boolean visit(EnumDeclaration node) {
		return false;
	}

	@Override
	public boolean visit(ExpressionMethodReference node) {
		return false;
	}

	@Override
	public boolean visit(ExpressionStatement node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(FieldAccess node) {
		IVariableBinding b = node.resolveFieldBinding();
		ITypeBinding tb = null;
		if (b != null) {
			tb = b.getDeclaringClass();
			if (tb != null) {
				tb = tb.getTypeDeclaration();
				if (tb.isLocal() || tb.getQualifiedName().isEmpty())
					return false;
			}
		}
		this.fullTokens.append(" ");
		this.partialTokens.append(" ");
		this.fqnTokens.append(" ");
		this.holeTokens.append(" ");
		node.getExpression().accept(this);
		String name = "." + node.getName().getIdentifier();
		this.partialTokens.append(" " + name + " ");
		this.holeTokens.append(" <blank>." + name + " ");
		if (b != null) {
			if (tb != null)
				name = getQualifiedName(tb.getTypeDeclaration()) + name;
			/*else
				name = "Array" + name;*/
		}
		this.fullTokens.append(" " + name + " ");
		this.fqnTokens.append(" " + name + " ");
		return false;
	}

	@Override
	public boolean visit(FieldDeclaration node) {
		return false;
	}

	@Override
	public boolean visit(ForStatement node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(IfStatement node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(ImportDeclaration node) {
		return false;
	}

	@Override
	public boolean visit(InfixExpression node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(Initializer node) {
		return false;
	}

	@Override
	public boolean visit(InstanceofExpression node) {
		this.fullTokens.append(" ");
		this.partialTokens.append(" ");
		node.getLeftOperand().accept(this);
		this.fullTokens.append(" <instanceof> ");
		this.partialTokens.append(" <instanceof> ");
		this.fqnTokens.append(" <instanceof> ");
		this.holeTokens.append(" <instanceof> ");
		String rtype = getResolvedType(node.getRightOperand()), utype = getUnresolvedType(node.getRightOperand());
		this.fullTokens.append(rtype + " ");
		this.fqnTokens.append(rtype + " ");
		this.partialTokens.append(utype + " ");
		this.holeTokens.append("<blank>." + utype + " ");
		return false;
	}

	@Override
	public boolean visit(LabeledStatement node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(LambdaExpression node) {
		return false;
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		if (node.getBody() != null && !node.getBody().statements().isEmpty())
			node.getBody().accept(this);
		return false;
	}

	@Override
	public boolean visit(MethodInvocation node) {
		if (node.getExpression() != null && node.getExpression() instanceof TypeLiteral) {
			TypeLiteral lit = (TypeLiteral) node.getExpression();
			String utype = getUnresolvedType(lit.getType()), rtype = getResolvedType(lit.getType());
			this.fullTokens.append(" " + rtype + ".class." + node.getName().getIdentifier() + "() ");
//			this.fqnTokens.append(" " + rtype + ".class." + node.getName().getIdentifier() + "() ");
			this.partialTokens.append(" " + utype + ".class." + node.getName().getIdentifier() + "(" + node.arguments().size() + ") ");
			this.holeTokens.append(" <blank>." + utype + ".class." + node.getName().getIdentifier() + "(" + node.arguments().size() + ") ");
		} else {
			IMethodBinding b = node.resolveMethodBinding();
			ITypeBinding tb = null;
			if (b != null) {
				tb = b.getDeclaringClass();
				if (tb != null) {
					tb = tb.getTypeDeclaration();
					if (tb.isLocal() || tb.getQualifiedName().isEmpty())
						return false;
				}
			}
			this.fullTokens.append(" ");
			this.fqnTokens.append(" ");
			this.partialTokens.append(" ");
			this.holeTokens.append(" ");
			if (node.getExpression() != null) {
				node.getExpression().accept(this);
			} else {
				if (tb != null) {
					this.partialTokens.append(" " + getName(tb) + " ");
					this.fullTokens.append(" " + getQualifiedName(tb) + " ");
					this.holeTokens.append(" <blank>." + getName(tb) + " ");
//					this.fqnTokens.append(" " + getQualifiedName(tb) + " ");
				} else {
					this.partialTokens.append(" this ");
					this.fullTokens.append(" this ");
					this.holeTokens.append(" this ");
//					this.fqnTokens.append(" this ");
				}
			}
			String name = "."+ node.getName().getIdentifier() + "(" + node.arguments().size() + ")";
			this.partialTokens.append(" " + name + " ");
			this.holeTokens.append(" <blank>." + name + " ");
			if (tb != null)
				name = getSignature(b.getMethodDeclaration());
			this.fullTokens.append(" " + name + " ");
//			this.fqnTokens.append(" " + name + " ");
		}
		for (int i = 0; i < node.arguments().size(); i++)
			((ASTNode) node.arguments().get(i)).accept(this);
		return false;
	}

	@Override
	public boolean visit(Modifier node) {
		return false;
	}

	@Override
	public boolean visit(NormalAnnotation node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(NullLiteral node) {
		this.fullTokens.append(" null ");
		this.partialTokens.append(" null ");
		this.holeTokens.append(" null ");
		this.fqnTokens.append(" null ");
		return false;
	}

	@Override
	public boolean visit(NumberLiteral node) {
		this.fullTokens.append(" number ");
		this.partialTokens.append(" number ");
		String value = node.getToken();
		this.holeTokens.append(" " + value + " ");
		this.fqnTokens.append(" " + value + " ");
		return false;
	}

	@Override
	public boolean visit(PackageDeclaration node) {
		return false;
	}

	@Override
	public boolean visit(ParenthesizedExpression node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(PostfixExpression node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(PrefixExpression node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(QualifiedName node) {
		IBinding b = node.resolveBinding();
		IVariableBinding vb = null;
		ITypeBinding tb = null;
		if (b != null) {
			if (b instanceof IVariableBinding) {
				vb = (IVariableBinding) b;
				tb = vb.getDeclaringClass();
				if (tb != null) {
					tb = tb.getTypeDeclaration();
					if (tb.isLocal() || tb.getQualifiedName().isEmpty())
						return false;
				}
			} else if (b instanceof ITypeBinding) {
				tb = ((ITypeBinding) b).getTypeDeclaration();
				if (tb.isLocal() || tb.getQualifiedName().isEmpty())
					return false;
				this.partialTokens.append(" " + node.getFullyQualifiedName() + " ");
				this.fullTokens.append(" " + getQualifiedName(tb) + " ");
				this.holeTokens.append(" <blank>." + node.getFullyQualifiedName() + " ");
				this.fqnTokens.append(" " + getQualifiedName(tb) + " ");
				return false;
			}
		} else {
			this.partialTokens.append(" " + node.getFullyQualifiedName() + " ");
			this.fullTokens.append(" " + node.getFullyQualifiedName() + " ");
			this.holeTokens.append(" <blank>." + node.getFullyQualifiedName() + " ");
			this.fqnTokens.append(" " + node.getFullyQualifiedName() + " ");
			return false;
		}
		node.getQualifier().accept(this);
		String name = "." + node.getName().getIdentifier();
		this.partialTokens.append(" " + name + " ");
		this.holeTokens.append(" <blank>." + name + " ");
		if (b != null) {
			if (b instanceof IVariableBinding) {
				if (tb != null)
					name = getQualifiedName(tb.getTypeDeclaration()) + name;
				/*else
					name = "Array" + name;*/
			}
		}
		this.fullTokens.append(" " + name + " ");
		this.fqnTokens.append(" " + name + " ");
		return false;
	}

	@Override
	public boolean visit(ReturnStatement node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(SimpleName node) {
		IBinding b = node.resolveBinding();
		if (b != null) {
			if (b instanceof IVariableBinding) {
				IVariableBinding vb = (IVariableBinding) b;
				ITypeBinding tb = vb.getType();
				if (tb != null) {
					tb = tb.getTypeDeclaration();
					if (tb.isLocal() || tb.getQualifiedName().isEmpty())
						return false;
					this.fullTokens.append(" " + getQualifiedName(tb) + " ");
					this.partialTokens.append(" " + getName(tb) + " ");
					this.holeTokens.append(" <blank> ");
					this.fqnTokens.append(" " + getQualifiedName(tb) + " ");
				}
			} else if (b instanceof ITypeBinding) {
				ITypeBinding tb = (ITypeBinding) b;
				tb = tb.getTypeDeclaration();
				if (tb.isLocal() || tb.getQualifiedName().isEmpty())
					return false;
				this.fullTokens.append(" " + getQualifiedName(tb) + " ");
				this.partialTokens.append(" " + getName(tb) + " ");
				this.holeTokens.append(" <blank> ");
				this.fqnTokens.append(" " + getQualifiedName(tb) + " ");
			}
		} else {
			this.fullTokens.append(" " + node.getIdentifier() + " ");
			this.partialTokens.append(" " + node.getIdentifier() + " ");
			this.holeTokens.append(" " + node.getIdentifier() + " ");
			this.fqnTokens.append(" " + node.getIdentifier() + " ");
		}
		return false;
	}

	@Override
	public boolean visit(SingleMemberAnnotation node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(SingleVariableDeclaration node) {
		ITypeBinding tb = node.getType().resolveBinding();
		if (tb != null && tb.getTypeDeclaration().isLocal())
			return false;
		String utype = getUnresolvedType(node.getType()), rtype = getResolvedType(node.getType());
		this.partialTokens.append(" " + utype + " ");
		this.fullTokens.append(" " + rtype + " ");
		this.holeTokens.append(" <blank> ");
		this.fqnTokens.append(" " + rtype + " ");
		if (node.getInitializer() != null) {
			this.partialTokens.append("= ");
			this.fullTokens.append("= ");
			this.holeTokens.append("= ");
			this.fqnTokens.append("= ");
			node.getInitializer().accept(this);
		}
		return false;
	}

	@Override
	public boolean visit(StringLiteral node) {
		this.fullTokens.append(" java.lang.String ");
		this.partialTokens.append(" java.lang.String ");
		this.holeTokens.append(" " + node.getEscapedValue() + " ");
		this.fqnTokens.append(" " + node.getEscapedValue() + " ");
		return false;
	}

	@Override
	public boolean visit(SuperConstructorInvocation node) {
		IMethodBinding b = node.resolveConstructorBinding();
		ITypeBinding tb = null;
		if (b != null && b.getDeclaringClass() != null)
			tb = b.getDeclaringClass().getTypeDeclaration();
		if (tb != null) {
			if (tb.isLocal() || tb.getQualifiedName().isEmpty())
				return false;
		}
		String name = "." + superClassName + "(" + node.arguments().size() + ")";
		this.partialTokens.append(" " + name + " ");
		this.holeTokens.append(" <blank>." + name + " ");
		if (tb != null)
			name = getSignature(b.getMethodDeclaration());
		this.fullTokens.append(" " + name + " ");
		this.fqnTokens.append(" " + name + " ");
		for (int i = 0; i < node.arguments().size(); i++)
			((ASTNode) node.arguments().get(i)).accept(this);
		return false;
	}

	@Override
	public boolean visit(SuperFieldAccess node) {
		IVariableBinding b = node.resolveFieldBinding();
		ITypeBinding tb = null;
		if (b != null && b.getDeclaringClass() != null) {
			tb = b.getDeclaringClass().getTypeDeclaration();
			if (tb.isLocal() || tb.getQualifiedName().isEmpty())
				return false;
			this.partialTokens.append(" " + getName(tb) + " ");
			this.fullTokens.append(" " + getQualifiedName(tb) + " ");
			this.holeTokens.append(" <blank> ");
			this.fqnTokens.append(" " + getQualifiedName(tb) + " ");
		} else {
			this.partialTokens.append(" super ");
			this.fullTokens.append(" super ");
			this.holeTokens.append(" super ");
			this.fqnTokens.append(" super ");
		}
		String name = "." + node.getName().getIdentifier();
		this.partialTokens.append(" " + name + " ");
		this.holeTokens.append(" <blank> ");
		if (tb != null)
			name = getQualifiedName(tb) + name;
		this.fullTokens.append(" " + name + " ");
		this.fqnTokens.append(" " + name + " ");
		return false;
	}

	@Override
	public boolean visit(SuperMethodInvocation node) {
		IMethodBinding b = node.resolveMethodBinding();
		ITypeBinding tb = null;
		if (b != null && b.getDeclaringClass() != null)
			tb = b.getDeclaringClass().getTypeDeclaration();
		if (tb != null) {
			if (tb.isLocal() || tb.getQualifiedName().isEmpty())
				return false;
			this.partialTokens.append(" " + getName(tb) + " ");
			this.fullTokens.append(" " + getQualifiedName(tb) + " ");
		} else {
			this.partialTokens.append(" super ");
			this.fullTokens.append(" super ");
		}
		String name = "." + node.getName().getIdentifier() + "(" + node.arguments().size() + ")";
		this.partialTokens.append(" " + name + " ");
		if (tb != null)
			name = getSignature(b.getMethodDeclaration());
		this.fullTokens.append(" " + name + " ");
		for (int i = 0; i < node.arguments().size(); i++)
			((ASTNode) node.arguments().get(i)).accept(this);
		return false;
	}

	@Override
	public boolean visit(SuperMethodReference node) {
		return false;
	}

	@Override
	public boolean visit(SwitchCase node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(SwitchStatement node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(SynchronizedStatement node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(ThisExpression node) {
		ITypeBinding b = node.resolveTypeBinding();
		if (b != null) {
			b = b.getTypeDeclaration();
			if (b.isLocal() || b.getQualifiedName().isEmpty())
				return false;
			this.partialTokens.append(" " + getName(b) + " ");
			this.fullTokens.append(" " + getQualifiedName(b) + " ");
			this.holeTokens.append(" <blank> ");
			this.fqnTokens.append(" " + getQualifiedName(b) + " ");
		} else {
			this.partialTokens.append(" this ");
			this.fullTokens.append(" this ");
			this.holeTokens.append(" this ");
			this.fqnTokens.append(" this ");
		}
		return false;
	}

	@Override
	public boolean visit(ThrowStatement node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(TryStatement node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		return false;
	}

	@Override
	public boolean visit(TypeDeclarationStatement node) {
		return false;
	}

	@Override
	public boolean visit(TypeLiteral node) {
		String utype = getUnresolvedType(node.getType()), rtype = getResolvedType(node.getType());
		this.fullTokens.append(" " + rtype + ".class ");
		this.partialTokens.append(" " + utype + ".class ");
		this.holeTokens.append(" <blank>." + rtype + ".class ");
		this.fqnTokens.append(" " + utype + ".class ");
		return false;
	}

	@Override
	public boolean visit(TypeMethodReference node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(TypeParameter node) {
		return super.visit(node);
	}
	
	@Override
	public boolean visit(VariableDeclarationExpression node) {
		ITypeBinding tb = node.getType().resolveBinding();
		if (tb != null && tb.getTypeDeclaration().isLocal())
			return false;
		String utype = getUnresolvedType(node.getType()), rtype = getResolvedType(node.getType());
		this.partialTokens.append(" " + utype + " ");
		this.fullTokens.append(" " + rtype + " ");
		this.holeTokens.append(" <blank>." + utype + " ");
		this.fqnTokens.append(" " + rtype + " ");
		for (int i = 0; i < node.fragments().size(); i++)
			((ASTNode) node.fragments().get(i)).accept(this);
		return false;
	}

	@Override
	public boolean visit(VariableDeclarationStatement node) {
		ITypeBinding tb = node.getType().resolveBinding();
		if (tb != null && tb.getTypeDeclaration().isLocal())
			return false;
		String utype = getUnresolvedType(node.getType()), rtype = getResolvedType(node.getType());
		this.partialTokens.append(" " + utype + " ");
		this.fullTokens.append(" " + rtype + " ");
		this.holeTokens.append(" <blank>." + utype + " ");
		this.fqnTokens.append(" " + rtype + " ");
		for (int i = 0; i < node.fragments().size(); i++)
			((ASTNode) node.fragments().get(i)).accept(this);
		return false;
	}

	@Override
	public boolean visit(VariableDeclarationFragment node) {
		Type type = getType(node);
		String utype = getUnresolvedType(type), rtype = getResolvedType(type);
		this.partialTokens.append(" " + utype + " ");
		this.fullTokens.append(" " + rtype + " ");
		this.holeTokens.append(" <blank>." + utype + " ");
		this.fqnTokens.append(" " + rtype + " ");
		if (node.getInitializer() != null) {
			this.partialTokens.append("= ");
			this.fullTokens.append("= ");
			node.getInitializer().accept(this);
		}
		return false;
	}

	@Override
	public boolean visit(WhileStatement node) {
		return super.visit(node);
	}
	
	@Override
	public boolean visit(ArrayType node) {
		return false;
	}
	
	@Override
	public boolean visit(IntersectionType node) {
		return false;
	}
	
	@Override
	public boolean visit(ParameterizedType node) {
		return false;
	}
	
	@Override
	public boolean visit(UnionType node) {
		return false;
	}
	
	@Override
	public boolean visit(NameQualifiedType node) {
		return false;
	}
	
	@Override
	public boolean visit(PrimitiveType node) {
		return false;
	}
	
	@Override
	public boolean visit(QualifiedType node) {
		return false;
	}
	
	@Override
	public boolean visit(SimpleType node) {
		return false;
	}
	
	@Override
	public boolean visit(WildcardType node) {
		return false;
	}

	private String getQualifiedName(ITypeBinding tb) {
		if (tb.isArray())
			return getQualifiedName(tb.getComponentType().getTypeDeclaration()) + getDimensions(tb.getDimensions());
		return tb.getQualifiedName();
	}

	private String getName(ITypeBinding tb) {
		if (tb.isArray())
			return getName(tb.getComponentType().getTypeDeclaration()) + getDimensions(tb.getDimensions());
		return tb.getName();
	}

}
