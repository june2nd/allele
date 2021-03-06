package alleles.examples.geneticProgramming

import alleles.environment.{Epic, GeneticAlgorithm}
import alleles.genotype.{Fitness, Join, Scheme, Variation}
import alleles.stages.{CrossoverStrategy, MutationStrategy, Selection}
import alleles.toolset.RRandom
import alleles.{Epoch, Population}

object Test extends App {

  implicit def doubleToValue(d: Double): Value = Value(d)

  implicit def intToValue(i: Int): Value = Value(i.toDouble)

  implicit def stringToVariable(s: String): Variable = Variable(s)

  def prettyTree(t: GPTree): String = t match {
    case Variable(name) => name
    case Value(v) => v.toString
    case Sin(a) => s"sin(${prettyTree(a)})"
    case Cos(a) => s"cos(${prettyTree(a)})"
    case Plus(a, b) => s"(${prettyTree(a)} + ${prettyTree(b)})"
    case Minus(a, b) => s"(${prettyTree(a)} - ${prettyTree(b)})"
    case Multiply(a, b) => s"(${prettyTree(a)} * ${prettyTree(b)})"
    case Divide(a, b) => s"(${prettyTree(a)} / ${prettyTree(b)})"
  }

  class GPTreeOps(generator: TreeGen, goal: GPTree) {
    implicit val join: Join[GPTree] = Join.pair(_.cross(_))

    implicit val variation: Variation[GPTree] = new TreeVariation(generator)

    implicit val scheme: Scheme[GPTree] = new TreeScheme(generator)
  }


  def arbFitness[A](goal: A, n: Int, varRange: Double)(implicit aCalc: Calc[A]): Fitness[GPTree] = (g: GPTree) => {
    aCalc.eval(goal)
  }

  def calcs(n: Int, varRanges: Map[String, Double]): List[Calc[GPTree]] =
    (1 to n).map(_ => Calc.tree(varRanges.view.mapValues(RRandom.inRange).toMap)).toList

  def treeFitness(goal: GPTree, calcs: List[Calc[GPTree]]): Fitness[GPTree] = g =>
    calcs.foldLeft(0.0) { case (sumError, c) => sumError + math.abs(c.eval(goal) - c.eval(g)) } / calcs.size

  val variables = List("x")
  val generator = new TreeGen(variables)

  val goal: GPTree = "x"
  //Plus(Sin("x"), Divide("x", 2))

  implicit val fintess = treeFitness(goal, calcs(100, variables.map(_ -> 100.0).toMap))

  val ops = new GPTreeOps(generator, goal)

  import ops._

  val operators = Epoch(
    Selection.tournament(20),
    CrossoverStrategy.parentsOrOffspring(0.5),
    MutationStrategy.repetitiveMutation(0.4, 0.2))

  val lastPop: Population[GPTree] =
    GeneticAlgorithm[GPTree].par.evolve(Epic(100, operators)).take(1000).compile.last.get

  import alleles.PopulationExtension
  import alleles.genotype.syntax._

  val best = lastPop.best
  println("Target: " + prettyTree(goal))
  println("Evolved: " + prettyTree(best))
  println("Fitness: " + best.fitness)
}

