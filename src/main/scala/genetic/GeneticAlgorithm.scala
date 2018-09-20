package genetic

import cats.Semigroup
import genetic.genotype.{Fitness, Modification}
import genetic.operators._

import scala.concurrent.duration.Duration
import scala.language.higherKinds

trait GeneticAlgorithm[F[_]] {
  /**
    * Evolves initial population with genetic operators for a finite number of iterations
    * @param settings Set of genetic operators which define iterative cycle
    * @param iterations Number of iterations
    * @return Evolved population
    */
  def evolve[G: Fitness: Semigroup: Modification](settings: AlgoSettings[G], iterations: Int): F[Population[G]] =
    evolve[G, Int](settings)(0, _ < iterations, _ + 1)

  /**
    * Evolves initial population with genetic operators for a provided time
    * Warning: is nondeterministic because of varying time of performance of a certain operation by CPU,
    *         thus can not be exactly reproduced with ReusableRandom
    * @param settings Set of genetic operators which define iterative cycle
    * @param duration Duration of evolving
    * @return Evolved population
    */
  def evolve[G: Fitness: Semigroup: Modification](settings: AlgoSettings[G], duration: Duration): F[Population[G]] = {
    val start = System.currentTimeMillis()
    evolve[G, Long](settings)(start, _ < start + duration.toMillis, _ => System.currentTimeMillis())
  }

  /**
    * Evolves initial population with genetic operators until the population
    * reaches certain fitness threshold with at least one representative
    * @param settings Set of genetic operators which define iterative cycle
    * @param fitnessThreshold Threshold to be crossed
    * @return Evolved population
    */
  def evolveUntilReached[G: Fitness: Semigroup: Modification](settings: AlgoSettings[G], fitnessThreshold: Int): F[Population[G]]

  protected def evolve[G: Fitness: Semigroup: Modification, B](settings: AlgoSettings[G])(start: B, until: B => Boolean, click: B => B): F[Population[G]]
}

case class AlgoSettings[G](initial: Population[G], selection: Selection, crossover: Crossover, mutation: Mutation) {
  def loop(population: Population[G]) (implicit f: Fitness[G], s: Semigroup[G], m: Modification[G]) =
    crossover.expand.apply(selection.expand.apply(population))
//    mutation.expand.apply(crossover.expand.apply(selection.applyExpanded(population)))
}
