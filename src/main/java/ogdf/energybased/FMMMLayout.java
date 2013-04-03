package ogdf.energybased;

/*
 * $Revision: 2565 $
 *
 * last checkin:
 *   $Author: gutwenger $
 *   $Date: 2012-07-07 17:14:54 +0200 (Sa, 07. Jul 2012) $
 ***************************************************************/
/**
 * \file \brief Implementation of Fast Multipole Multilevel Method (FM^3).
 *
 * \author Stefan Hachul
 *
 * \par License: This file is part of the Open Graph Drawing Framework (OGDF).
 *
 * \par Copyright (C)<br> See README.txt in the root directory of the OGDF installation for details.
 *
 * \par This program is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License Version 2 or 3 as published by the Free Software Foundation; see the file LICENSE.txt included in the
 * packaging of this file for details.
 *
 * \par This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * \par You should have received a copy of the GNU General Public License along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * \see http://www.gnu.org/copyleft/gpl.html
 **************************************************************
 */
import java.lang.*;
import java.util.*;
import ogdf.basic.*;

public class FMMMLayout
{

    //! Possible page formats.
    enum PageFormatType
    {
        pfPortrait, //!< A4 portrait page.
        pfLandscape, //!< A4 landscape page.
        pfSquare     //!< Square format.
    };

    //! Trade-off between run-time and quality.
    enum QualityVsSpeed
    {
        qvsGorgeousAndEfficient, //!< Best quality.
        qvsBeautifulAndFast, //!< Medium quality and speed.
        qvsNiceAndIncredibleSpeed //!< Best speed.
    };

    //! Specifies how the length of an edge is measured.
    enum EdgeLengthMeasurement
    {
        elmMidpoint, //!< Measure from center point of edge end points.
        elmBoundingCircle //!< Measure from border of circle s surrounding edge end points.
    };

    //! Specifies which positions for a node are allowed.
    enum AllowedPositions
    {
        apAll,
        apInteger,
        apExponent
    };

    //! Specifies in which case it is allowed to tip over drawings of connected components.
    enum TipOver
    {
        toNone,
        toNoGrowingRow,
        toAlways
    };

    //! Specifies how connected components are sorted before the packing algorithm is applied.
    enum PreSort
    {
        psNone, //!< Do not presort.
        psDecreasingHeight, //!< Presort by decreasing height of components.
        psDecreasingWidth   //!< Presort by decreasing width of components.
    };

    //! Specifies how sun nodes of galaxies are selected.
    public enum GalaxyChoice
    {
        gcUniformProb,
        gcNonUniformProbLowerMass,
        gcNonUniformProbHigherMass
    };

    //! Specifies how MaxIterations is changed in subsequent multilevels.
    enum MaxIterChange
    {
        micConstant,
        micLinearlyDecreasing,
        micRapidlyDecreasing
    };

    //! Specifies how the initial placement is generated.
    public enum InitialPlacementMult
    {
        ipmSimple,
        ipmAdvanced
    };

    //! Specifies the force model.
    enum ForceModel
    {
        fmFruchtermanReingold, //!< The force-model by Fruchterman, Reingold.
        fmEades, //!< The force-model by Eades.
        fmNew                  //!< The new force-model.
    };

    //! Specifies how to calculate repulsive forces.
    enum RepulsiveForcesMethod
    {
        rfcExact, //!< Exact calculation.
        rfcGridApproximation, //!< Grid approximation.
        rfcNMM                //!< Calculation as for new multipole method.
    };

    //! Specifies the stop criterion.
    enum StopCriterion
    {
        scFixedIterations, //!< Stop if fixedIterations() is reached.
        scThreshold, //!< Stop if threshold() is reached.
        scFixedIterationsOrThreshold //!< Stop if fixedIterations() or threshold() is reached.
    };

    //! Specifies how the initial placement is done.
    enum InitialPlacementForces
    {
        ipfUniformGrid, //!< Uniform placement on a grid.
        ipfRandomTime, //!< Random placement (based on current time).
        ipfRandomRandIterNr, //!< Random placement (based on randIterNr()).
        ipfKeepPositions     //!< No change in placement.
    };

    //! Specifies how the reduced bucket quadtree is constructed.
    enum ReducedTreeConstruction
    {
        rtcPathByPath, //!< Path-by-path construction.
        rtcSubtreeBySubtree //!< Subtree-by-subtree construction.
    };

    //! Specifies how to calculate the smallest quadratic cell surrounding particles of a node in the reduced bucket quadtree.
    enum SmallestCellFinding
    {
        scfIteratively, //!< Iteratively (in constant time).
        scfAluru        //!< According to formula by Aluru et al. (in constant time).
    };

    //! Returns the runtime (=CPU-time) of the layout algorithm in seconds.
    double getCpuTime()
    {
        return time_total;
    }

    /**
     * @}
     * @name High-level options Allow to specify the most relevant parameters.
     * @{
     */
    //! Returns the current setting of option useHighLevelOptions.
    /**
     * If set to true, the high-level options are used to set all low-level options. Usually, it is sufficient just to
     * set high-level options; if you want to be more specific, set this parameter to false and set the low level
     * options.
     */
    boolean useHighLevelOptions()
    {
        return m_useHighLevelOptions;
    }

    //! Sets the option useHighLevelOptions to \a uho.
    void useHighLevelOptions(boolean uho)
    {
        m_useHighLevelOptions = uho;
    }

    //! Sets single level option, no multilevel hierarchy is created if b == true
    void setSingleLevel(boolean b)
    {
        m_singleLevel = b;
    }

    //! Returns the current setting of option pageFormat.
    /**
     * This option defines the desired aspect ratio of the drawing area. - \a pfPortrait: A4 page in portrait
     * orientation - \a pfLandscape: A4 page in landscape orientation - \a pfSquare: square page format
     */
    PageFormatType pageFormat()
    {
        return m_pageFormat;
    }

    //! Sets the option pageRatio to \a t.
    void pageFormat(PageFormatType t)
    {
        m_pageFormat = t;
    }

    //! Returns the current setting of option unitEdgeLength.
    double unitEdgeLength()
    {
        return m_unitEdgeLength;
    }

    //! Sets the option unitEdgeLength to \a x.
    void unitEdgeLength(double x)
    {
        m_unitEdgeLength = ((x > 0.0) ? x : 1);
    }

    //! Returns the current setting of option newInitialPlacement.
    /**
     * This option defines if the initial placement of the nodes at the coarsest multilevel is varied for each distinct
     * call of FMMMLayout or keeps always the same.
     */
    boolean newInitialPlacement()
    {
        return m_newInitialPlacement;
    }

    //! Sets the option newInitialPlacement to \a nip.
    void newInitialPlacement(boolean nip)
    {
        m_newInitialPlacement = nip;
    }

    //! Returns the current setting of option qualityVersusSpeed.
    /**
     * Indicates if the algorithm is tuned either for best quality or best speed. - \a qvsGorgeousAndEfficient: gorgeous
     * quality and efficient speed - \a qvsBeautifulAndFast: beautiful quality and fast speed - \a
     * qvsNiceAndIncredibleSpeed: nice quality and incredible speed
     */
    QualityVsSpeed qualityVersusSpeed()
    {
        return m_qualityVersusSpeed;
    }

    //! Sets the option qualityVersusSpeed to \a qvs.
    void qualityVersusSpeed(QualityVsSpeed qvs)
    {
        m_qualityVersusSpeed = qvs;
    }

    /**
     * @}
     * @name General low-level options The low-level options in this and the following sections are meant for experts or
     * interested people only.
     * @{
     */
    //! Sets the seed of the random number generator.
    void randSeed(int p)
    {
        m_randSeed = ((0 <= p) ? p : 1);
    }

    //! Returns the seed of the random number generator.
    int randSeed()
    {
        return m_randSeed;
    }

    //! Returns the current setting of option edgeLengthMeasurement.
    /**
     * This option indicates how the length of an edge is measured. Possible values: - \a elmMidpoint: from center to
     * center - \a elmBoundingCircle: the distance between the two tight circles bounding the graphics of two adjacent
     * nodes
     */
    EdgeLengthMeasurement edgeLengthMeasurement()
    {
        return m_edgeLengthMeasurement;
    }

    //! Sets the option edgeLengthMeasurement to \a elm.
    void edgeLengthMeasurement(EdgeLengthMeasurement elm)
    {
        m_edgeLengthMeasurement = elm;
    }

    //! Returns the current setting of option allowedPositions.
    /**
     * This option defines which positions for a node are allowed. Possibly values: - \a apAll: every position is
     * allowed - \a apInteger: only integer positions in the range depending on the number of nodes - \a apExponent:
     * only integer positions in the range of -2^MaxIntPosExponent to 2^MaxIntPosExponent
     */
    AllowedPositions allowedPositions()
    {
        return m_allowedPositions;
    }

    //! Sets the option allowedPositions to \a ap.
    void allowedPositions(AllowedPositions ap)
    {
        m_allowedPositions = ap;
    }

    //! Returns the current setting of option maxIntPosExponent.
    /**
     * This option defines the exponent used if allowedPositions() == \a apExponent.
     */
    int maxIntPosExponent()
    {
        return m_maxIntPosExponent;
    }

    //! Sets the option maxIntPosExponent to \a e.
    void maxIntPosExponent(int e)
    {
        m_maxIntPosExponent = (((e >= 31) && (e <= 51)) ? e : 31);
    }

    /**
     * @}
     * @name Options for the divide et impera step
     * @{
     */
    //! Returns the current setting of option pageRatio.
    /**
     * This option defines the desired aspect ratio of the rectangular drawing area.
     */
    double pageRatio()
    {
        return m_pageRatio;
    }

    //! Sets the option pageRatio to \a r.
    void pageRatio(double r)
    {
        m_pageRatio = ((r > 0) ? r : 1);
    }

    //! Returns the current setting of option stepsForRotatingComponents.
    /**
     * This options determines the number of times each connected component is rotated with angles between 0 and 90
     * degree to obtain a bounding rectangle with small area.
     */
    int stepsForRotatingComponents()
    {
        return m_stepsForRotatingComponents;
    }

    //! Sets the option stepsForRotatingComponents to \a n.
    void stepsForRotatingComponents(int n)
    {
        m_stepsForRotatingComponents = ((0 <= n) ? n : 0);
    }

    //! Returns the current setting of option tipOverCCs.
    /**
     * Defines in which case it is allowed to tip over drawings of connected components. Possible values: - \a toNone:
     * not allowed at all - \a toNoGrowingRow: only if the height of the packing row does not grow - \a toAlways: always
     * allowed
     */
    TipOver tipOverCCs()
    {
        return m_tipOverCCs;
    }

    //! Sets the option tipOverCCs to \a to.
    void tipOverCCs(TipOver to)
    {
        m_tipOverCCs = to;
    }

    //! Returns the  minimal distance between connected components.
    double minDistCC()
    {
        return m_minDistCC;
    }

    //! Sets the  minimal distance between connected components to \a x.
    void minDistCC(double x)
    {
        m_minDistCC = ((x > 0) ? x : 1);
    }

    //! Returns the current setting of option presortCCs.
    /**
     * This option defines if the connected components are sorted before the packing algorithm is applied. Possible
     * values: - \a psNone: no sorting - \a psDecreasingHeight: sorted by decreasing height - \a psDecreasingWidth:
     * sorted by decreasing width
     */
    PreSort presortCCs()
    {
        return m_presortCCs;
    }

    //! Sets the option presortCCs to \a ps.
    void presortCCs(PreSort ps)
    {
        m_presortCCs = ps;
    }

    /**
     * @}
     * @name Options for the multilevel step
     * @{
     */
    //! Returns the current setting of option minGraphSize.
    /**
     * This option determines the number of nodes of a graph in the multilevel representation for which no more
     * collapsing of galaxies is performed (i.e. the graph at the highest level).
     */
    int minGraphSize()
    {
        return m_minGraphSize;
    }

    //! Sets the option minGraphSize to \a n.
    void minGraphSize(int n)
    {
        m_minGraphSize = ((n >= 2) ? n : 2);
    }

    //! Returns the current setting of option galaxyChoice.
    /**
     * This option defines how sun nodes of galaxies are selected. Possible values: - \a gcUniformProb: selecting by
     * uniform random probability - \a gcNonUniformProbLowerMass: selecting by non-uniform probability depending on the
     * star masses (prefering nodes with lower star mass) - \a gcNonUniformProbHigherMass: as above but prefering nodes
     * with higher star mass
     */
    GalaxyChoice galaxyChoice()
    {
        return m_galaxyChoice;
    }

    //! Sets the option galaxyChoice to \a gc.
    void galaxyChoice(GalaxyChoice gc)
    {
        m_galaxyChoice = gc;
    }

    //! Returns the current setting of option randomTries.
    /**
     * This option defines the number of tries to get a random node with minimal star mass (used in case of
     * galaxyChoice() == gcNonUniformProbLowerMass and galaxyChoice() == gcNonUniformProbHigherMass).
     */
    int randomTries()
    {
        return m_randomTries;
    }

    //! Sets the option randomTries to \a n.
    void randomTries(int n)
    {
        m_randomTries = ((n >= 1) ? n : 1);
    }

    //! Returns the current setting of option maxIterChange.
    /**
     * This option defines how MaxIterations is changed in subsequent multilevels. Possible values: - \a micConstant:
     * kept constant at the force calculation step at every level - \a micLinearlyDecreasing: linearly decreasing from
     * MaxIterFactor*FixedIterations to FixedIterations - \a micRapidlyDecreasing: rapdily decreasing from
     * MaxIterFactor*FixedIterations to FixedIterations
     */
    MaxIterChange maxIterChange()
    {
        return m_maxIterChange;
    }

    //! Sets the option maxIterChange to \a mic.
    void maxIterChange(MaxIterChange mic)
    {
        m_maxIterChange = mic;
    }

    //! Returns the current setting of option maxIterFactor.
    /**
     * This option defines the factor used for decrasing MaxIterations (in case of maxIterChange() ==
     * micLinearlyDecreasing or maxIterChange() == micRapidlyDecreasing).
     */
    int maxIterFactor()
    {
        return m_maxIterFactor;
    }

    //! Sets the option maxIterFactor to \a f.
    void maxIterFactor(int f)
    {
        m_maxIterFactor = ((f >= 1) ? f : 1);
    }

    //! Returns the current setting of option initialPlacementMult.
    /**
     * This option defines how the initial placement is generated. Possible values: - \a ipmSimple: only using
     * information about placement of nodes on higher levels - \a ipmAdvanced: using additional information about the
     * placement of all inter - \a solar system nodes
     */
    InitialPlacementMult initialPlacementMult()
    {
        return m_initialPlacementMult;
    }

    //! Sets the option initialPlacementMult to \a ipm.
    void initialPlacementMult(InitialPlacementMult ipm)
    {
        m_initialPlacementMult = ipm;
    }

    /**
     * @}
     * @name Options for the force calculation step
     * @{
     */
    //! Returns the used force model.
    /**
     * Possibly values: - \a fmFruchtermanReingold: model of Fruchterman and Reingold - \a fmEades: model of Eades - \a
     * fmNew: new model
     */
    ForceModel forceModel()
    {
        return m_forceModel;
    }

    //! Sets the used force model to \a fm.
    void forceModel(ForceModel fm)
    {
        m_forceModel = fm;
    }

    //! Returns the strength of the springs.
    double springStrength()
    {
        return m_springStrength;
    }

    //! Sets the strength of the springs to \a x.
    void springStrength(double x)
    {
        m_springStrength = ((x > 0) ? x : 1);
    }

    //! Returns the strength of the repulsive forces.
    double repForcesStrength()
    {
        return m_repForcesStrength;
    }

    //! Sets the strength of the repulsive forces to \a x.
    void repForcesStrength(double x)
    {
        m_repForcesStrength = ((x > 0) ? x : 1);
    }

    //! Returns the current setting of option repulsiveForcesCalculation.
    /**
     * This option defines how to calculate repulsive forces. Possible values: - \a rfcExact: exact calculation (slow) -
     * \a rfcGridApproximation: grid approxiamtion (inaccurate) - \a rfcNMM: like in NMM (= New Multipole Method; fast
     * and accurate)
     */
    RepulsiveForcesMethod repulsiveForcesCalculation()
    {
        return m_repulsiveForcesCalculation;
    }

    //! Sets the option repulsiveForcesCalculation to \a rfc.
    void repulsiveForcesCalculation(RepulsiveForcesMethod rfc)
    {
        m_repulsiveForcesCalculation = rfc;
    }

    //! Returns the stop criterion.
    /**
     * Possible values: - \a rscFixedIterations: stop if fixedIterations() is reached - \a rscThreshold: stop if
     * threshold() is reached - \a rscFixedIterationsOrThreshold: stop if fixedIterations() or threshold() is reached
     */
    StopCriterion stopCriterion()
    {
        return m_stopCriterion;
    }

    //! Sets the stop criterion to \a rsc.
    void stopCriterion(StopCriterion rsc)
    {
        m_stopCriterion = rsc;
    }

    //! Returns the threshold for the stop criterion.
    /**
     * (If the average absolute value of all forces in an iteration is less then threshold() then stop.)
     */
    double threshold()
    {
        return m_threshold;
    }

    //! Sets the threshold for the stop criterion to \a x.
    void threshold(double x)
    {
        m_threshold = ((x > 0) ? x : 0.1);
    }

    //! Returns the fixed number of iterations for the stop criterion.
    int fixedIterations()
    {
        return m_fixedIterations;
    }

    //! Sets the fixed number of iterations for the stop criterion to \a n.
    void fixedIterations(int n)
    {
        m_fixedIterations = ((n >= 1) ? n : 1);
    }

    //! Returns the scaling factor for the forces.
    double forceScalingFactor()
    {
        return m_forceScalingFactor;
    }

    //! Sets the scaling factor for the forces to \ f.
    void forceScalingFactor(double f)
    {
        m_forceScalingFactor = ((f > 0) ? f : 1);
    }

    //! Returns the current setting of option coolTemperature.
    /**
     * If set to true, forces are scaled by coolValue()^(actual iteration) * forceScalingFactor(); otherwise forces are
     * scaled by forceScalingFactor().
     */
    boolean coolTemperature()
    {
        return m_coolTemperature;
    }

    //! Sets the option coolTemperature to \a b.
    void coolTemperature(boolean b)
    {
        m_coolTemperature = b;
    }

    //! Returns the current setting of option coolValue.
    /**
     * This option defines the value by which forces are decreased if coolTemperature is true.
     */
    double coolValue()
    {
        return m_coolValue;
    }

    //! Sets the option coolValue to \a x.
    void coolValue(double x)
    {
        m_coolValue = (((x > 0) && (x <= 1)) ? x : 0.99);
    }

    //! Returns the current setting of option initialPlacementForces.
    /**
     * This option defines how the initial placement is done. Possible values: - \a ipfUniformGrid: uniform on a grid -
     * \a ipfRandomTime: random based on actual time - \a ipfRandomRandIterNr: random based on randIterNr() - \a
     * ipfKeepPositions: no change in placement
     */
    InitialPlacementForces initialPlacementForces()
    {
        return m_initialPlacementForces;
    }

    //! Sets the option initialPlacementForces to \a ipf.
    void initialPlacementForces(InitialPlacementForces ipf)
    {
        m_initialPlacementForces = ipf;
    }

    /**
     * @}
     * @name Options for the postprocessing step
     * @{
     */
    //! Returns the current setting of option resizeDrawing.
    /**
     * If set to true, the resulting drawing is resized so that the average edge length is the desired edge length times
     * resizingScalar().
     */
    boolean resizeDrawing()
    {
        return m_resizeDrawing;
    }

    //! Sets the option resizeDrawing to \a b.
    void resizeDrawing(boolean b)
    {
        m_resizeDrawing = b;
    }

    //! Returns the current setting of option resizingScalar.
    /**
     * This option defines a parameter to scale the drawing if resizeDrawing() is true.
     */
    double resizingScalar()
    {
        return m_resizingScalar;
    }

    //! Sets the option resizingScalar to \a s.
    void resizingScalar(double s)
    {
        m_resizingScalar = ((s > 0) ? s : 1);
    }

    //! Returns the number of iterations for fine tuning.
    int fineTuningIterations()
    {
        return m_fineTuningIterations;
    }

    //! Sets the number of iterations for fine tuning to \a n.
    void fineTuningIterations(int n)
    {
        m_fineTuningIterations = ((n >= 0) ? n : 0);
    }

    //! Returns the curent setting of option fineTuneScalar.
    /**
     * This option defines a parameter for scaling the forces in the fine-tuning iterations.
     */
    double fineTuneScalar()
    {
        return m_fineTuneScalar;
    }

    //! Sets the option fineTuneScalar to \a s
    void fineTuneScalar(double s)
    {
        m_fineTuneScalar = ((s >= 0) ? s : 1);
    }

    //! Returns the current setting of option adjustPostRepStrengthDynamically.
    /**
     * If set to true, the strength of the repulsive force field is calculated dynamically by a formula depending on the
     * number of nodes; otherwise the strength are scaled by PostSpringStrength and PostStrengthOfRepForces.
     */
    boolean adjustPostRepStrengthDynamically()
    {
        return m_adjustPostRepStrengthDynamically;
    }

    //! Sets the option adjustPostRepStrengthDynamically to \a b.
    void adjustPostRepStrengthDynamically(boolean b)
    {
        m_adjustPostRepStrengthDynamically = b;
    }

    //! Returns the strength of the springs in the postprocessing step.
    double postSpringStrength()
    {
        return m_postSpringStrength;
    }

    //! Sets the strength of the springs in the postprocessing step to \a x.
    void postSpringStrength(double x)
    {
        m_postSpringStrength = ((x > 0) ? x : 1);
    }

    //! Returns the strength of the repulsive forces in the postprocessing step.
    double postStrengthOfRepForces()
    {
        return m_postStrengthOfRepForces;
    }

    //! Sets the strength of the repulsive forces in the postprocessing step to \a x.
    void postStrengthOfRepForces(double x)
    {
        m_postStrengthOfRepForces = ((x > 0) ? x : 1);
    }

    /**
     * @}
     * @name Options for repulsive force approximation methods
     * @{
     */
    //! Returns the current setting of option frGridQuotient.
    /**
     * The number k of rows and columns of the grid is sqrt(|V|) / frGridQuotient(). (Note that in
     * [Fruchterman,Reingold] frGridQuotient is 2.)
     */
    int frGridQuotient()
    {
        return m_frGridQuotient;
    }

    //! Sets the option frGridQuotient to \a p.
    void frGridQuotient(int p)
    {
        m_frGridQuotient = ((0 <= p) ? p : 2);
    }

    //! Returns the current setting of option nmTreeConstruction.
    /**
     * This option defines how the reduced bucket quadtree is constructed. Possible values: - \a rtcPathByPath: path by
     * path construction - \a rtcSubtreeBySubtree: subtree by subtree construction
     */
    ReducedTreeConstruction nmTreeConstruction()
    {
        return m_NMTreeConstruction;
    }

    //! Sets the option nmTreeConstruction to \a rtc.
    void nmTreeConstruction(ReducedTreeConstruction rtc)
    {
        m_NMTreeConstruction = rtc;
    }

    //! Returns the current setting of option nmSmallCell.
    /**
     * This option defines how the smallest quadratic cell that surrounds the particles of a node in the reduced bucket
     * quadtree is calculated. Possible values: - \a scfIteratively: iteratively (in constant time) - \a scfAluru: by
     * the formula by Aluru et al. (in constant time)
     */
    SmallestCellFinding nmSmallCell()
    {
        return m_NMSmallCell;
    }

    //! Sets the option nmSmallCell to \a scf.
    void nmSmallCell(SmallestCellFinding scf)
    {
        m_NMSmallCell = scf;
    }

    //! Returns the current setting of option nmParticlesInLeaves.
    /**
     * Defines the maximal number of particles that are contained in a leaf of the reduced bucket quadtree.
     */
    int nmParticlesInLeaves()
    {
        return m_NMParticlesInLeaves;
    }

    //! Sets the option nmParticlesInLeaves to \a n.
    void nmParticlesInLeaves(int n)
    {
        m_NMParticlesInLeaves = ((n >= 1) ? n : 1);
    }

    //! Returns the precision \a p for the <i>p</i>-term multipole expansions.
    int nmPrecision()
    {
        return m_NMPrecision;
    }

    //! Sets the precision for the multipole expansions to \ p.
    void nmPrecision(int p)
    {
        m_NMPrecision = ((p >= 1) ? p : 1);
    }
    //! @}
//FIXME private:
    //high level options
    boolean m_useHighLevelOptions; //!< The option for using high-level options.
    PageFormatType m_pageFormat; //!< The option for the page format.
    double m_unitEdgeLength; //!< The unit edge length.
    boolean m_newInitialPlacement; //!< The option for new initial placement.
    QualityVsSpeed m_qualityVersusSpeed; //!< The option for quality-vs-speed trade-off.
    //low level options
    //general options
    int m_randSeed; //!< The random seed.
    EdgeLengthMeasurement m_edgeLengthMeasurement; //!< The option for edge length measurement.
    AllowedPositions m_allowedPositions; //!< The option for allowed positions.
    int m_maxIntPosExponent; //!< The option for the used	exponent.
    //options for divide et impera step
    double m_pageRatio; //!< The desired page ratio.
    int m_stepsForRotatingComponents; //!< The number of rotations.
    TipOver m_tipOverCCs; //!< Option for tip-over of connected components.
    double m_minDistCC; //!< The separation between connected components.
    PreSort m_presortCCs; //!< The option for presorting connected components.
    //options for multilevel step
    boolean m_singleLevel; //!< Option for pure single level.
    int m_minGraphSize; //!< The option for minimal graph size.
    GalaxyChoice m_galaxyChoice; //!< The selection of galaxy nodes.
    int m_randomTries; //!< The number of random tries.
    MaxIterChange m_maxIterChange; //!< The option for how to change MaxIterations.
    //!< If maxIterChange != micConstant, the iterations are decreased
    //!< depending on the level, starting from
    //!< ((maxIterFactor()-1) * fixedIterations())
    int m_maxIterFactor; //!< The factor used for decreasing MaxIterations.
    InitialPlacementMult m_initialPlacementMult; //!< The option for creating initial placement.
    //options for force calculation step
    ForceModel m_forceModel; //!< The used force model.
    double m_springStrength; //!< The strengths of springs.
    double m_repForcesStrength; //!< The strength of repulsive forces.
    RepulsiveForcesMethod m_repulsiveForcesCalculation; //!< Option for how to calculate repulsive forces.
    StopCriterion m_stopCriterion; //!< The stop criterion.
    double m_threshold; //!< The threshold for the stop criterion.
    int m_fixedIterations; //!< The fixed number of iterations for the stop criterion.
    double m_forceScalingFactor; //!< The scaling factor for the forces.
    boolean m_coolTemperature; //!< The option for how to scale forces.
    double m_coolValue; //!< The value by which forces are decreased.
    InitialPlacementForces m_initialPlacementForces; //!< The option for how the initial placement is done.
    //options for postprocessing step
    boolean m_resizeDrawing; //!< The option for resizing the drawing.
    double m_resizingScalar; //!< Parameter for resizing the drawing.
    int m_fineTuningIterations; //!< The number of iterations for fine tuning.
    double m_fineTuneScalar; //!< Parameter for scaling forces during fine tuning.
    boolean m_adjustPostRepStrengthDynamically; //!< The option adjustPostRepStrengthDynamically.
    double m_postSpringStrength; //!< The strength of springs during postprocessing.
    double m_postStrengthOfRepForces; //!< The strength of repulsive forces during postprocessing.
    //options for repulsive force approximation methods
    int m_frGridQuotient; //!< The grid quotient.
    ReducedTreeConstruction m_NMTreeConstruction; //!< The option for how to construct reduced bucket quadtree.
    SmallestCellFinding m_NMSmallCell; //!< The option for how to calculate smallest quadtratic cells.
    int m_NMParticlesInLeaves; //!< The maximal number of particles in a leaf.
    int m_NMPrecision; //!< The precision for multipole expansions.
    //other variables
    double max_integer_position; //!< The maximum value for an integer position.
    double cool_factor; //!< Needed for scaling the forces if coolTemperature is true.
    double average_ideal_edgelength; //!< Measured from center to center.
    double boxlength; //!< Holds the length of the quadratic comput. box.
    int number_of_components; //!< The number of components of the graph.
    DPoint down_left_corner; //!< Holds down left corner of the comput. box.
    NodeArray<Double> radius; //!< Holds the radius of the surrounding circle for each node.
    double time_total; //!< The runtime (=CPU-time) of the algorithm in seconds.
    FruchtermanReingold FR; //!< Class for repulsive force calculation (Fruchterman, Reingold).
    NMM NM; //!< Class for repulsive force calculation.

    FMMMLayout()
    {
        initialize_all_options();
    }

//--------------------------- most important functions --------------------------------
    void call(GraphAttributes GA)
    {
        Graph G = GA.constGraph();
        EdgeArray<Double> edgelength = new EdgeArray<Double>(G, 1.0);
        call(GA, edgelength);
    }

    /*void call(ClusterGraphAttributes &GA)
     {
     Graph &G = GA.constGraph();
     //compute depth of cluster tree, also sets cluster depth values
     ClusterGraph &CG = GA.constClusterGraph();
     int cdepth = CG.treeDepth();
     EdgeArray<double> edgeLength(G);
     //compute lca of end vertices for each edge
     edge e;
     forall_edges(e, G)
     {
     edgeLength[e] = cdepth - CG.clusterDepth(CG.commonCluster(e->source(),e->target())) + 1;
     OGDF_ASSERT(edgeLength[e] > 0)
     }
     call(GA,edgeLength);
     GA.updateClusterPositions();
     }*/
    void call(GraphAttributes GA, EdgeArray<Double> edgeLength)
    {
        Graph G = GA.constGraph();
        NodeArray<NodeAttributes> A = new NodeArray<NodeAttributes>(G);       //stores the attributes of the nodes (given by L)
        EdgeArray<EdgeAttributes> E = new EdgeArray<EdgeAttributes>(G);       //stores the edge attributes of G
        Graph G_reduced = new Graph();                      //stores a undirected simple and loopfree copy
        //of G
        EdgeArray<EdgeAttributes> E_reduced = new EdgeArray<EdgeAttributes>();  //stores the edge attributes of G_reduced
        NodeArray<NodeAttributes> A_reduced = new NodeArray<NodeAttributes>();  //stores the node attributes of G_reduced

        if (G.numberOfNodes() > 1)
        {
            GA.clearAllBends();//all edges are straight-line
            if (useHighLevelOptions())
            {
                update_low_level_options_due_to_high_level_options_settings();
            }
            import_NodeAttributes(G, GA, A);
            import_EdgeAttributes(G, edgeLength, E);

            /*double t_total;
             usedTime(t_total);*/
            max_integer_position = Math.pow(2.0, maxIntPosExponent());
            init_ind_ideal_edgelength(G, A, E);
            make_simple_loopfree(G, A, E, G_reduced, A_reduced, E_reduced);
            call_DIVIDE_ET_IMPERA_step(G_reduced, A_reduced, E_reduced);
            if (allowedPositions() != AllowedPositions.apAll)
            {
                make_positions_integer(G_reduced, A_reduced);
            }
            //time_total = usedTime(t_total);

            export_NodeAttributes(G_reduced, A_reduced, GA);
        }
        else //trivial cases
        {
            if (G.numberOfNodes() == 1)
            {
                node v = G.firstNode();
                GA.setX(v, 0.0);
                GA.setY(v, 0.0);
            }
        }
    }


    /*void call(GraphAttributes &AG, char* ps_file)
     {
     call(AG);
     create_postscript_drawing(AG,ps_file);
     }


     void call(
     GraphAttributes &AG,
     EdgeArray<double> &edgeLength,
     char* ps_file)
     {
     call(AG,edgeLength);
     create_postscript_drawing(AG,ps_file);
     }*/
    void call_DIVIDE_ET_IMPERA_step(
            Graph G,
            NodeArray<NodeAttributes> A,
            EdgeArray<EdgeAttributes> E)
    {
        NodeArray<Integer> component = new NodeArray<Integer>(G); //holds for each node the index of its component
        number_of_components = SimpleGraphAlg.connectedComponents(G, component);//calculate components of G
        List<Graph> G_sub = new ArrayList<Graph>(number_of_components);
        List<NodeArray<NodeAttributes>> A_sub = new ArrayList<NodeArray<NodeAttributes>>(number_of_components);
        List<EdgeArray<EdgeAttributes>> E_sub = new ArrayList<EdgeArray<EdgeAttributes>>(number_of_components);
        create_maximum_connected_subGraphs(G, A, E, G_sub, A_sub, E_sub, component);

        if (number_of_components == 1)
        {
            call_MULTILEVEL_step_for_subGraph(G_sub.get(0), A_sub.get(0), E_sub.get(0), -1);
        }
        else
        {
            for (int i = 0; i < number_of_components; i++)
            {
                call_MULTILEVEL_step_for_subGraph(G_sub.get(i), A_sub.get(i), E_sub.get(i), i);
            }
        }

        pack_subGraph_drawings(A, G_sub, A_sub);
        //delete_all_subGraphs(G_sub,A_sub,E_sub);
    }

    void call_MULTILEVEL_step_for_subGraph(
            Graph G,
            NodeArray<NodeAttributes> A,
            EdgeArray<EdgeAttributes> E,
            int comp_index)
    {
        Multilevel Mult = new Multilevel();

        int max_level = 30;//sufficient for all graphs with upto pow(2,30) nodes!
        //adapt mingraphsize such that no levels are created beyond input graph.
        if (m_singleLevel)
        {
            m_minGraphSize = G.numberOfNodes();
        }
        List<Graph> G_mult_ptr = new ArrayList<Graph>(max_level + 1);
        List<NodeArray<NodeAttributes>> A_mult_ptr = new ArrayList<NodeArray<NodeAttributes>>(max_level + 1);
        List<EdgeArray<EdgeAttributes>> E_mult_ptr = new ArrayList<EdgeArray<EdgeAttributes>>(max_level + 1);

        Mult.create_multilevel_representations(G, A, E, randSeed(),
                galaxyChoice(), minGraphSize(),
                randomTries(), G_mult_ptr, A_mult_ptr,
                E_mult_ptr, max_level);

        for (int i = max_level; i >= 0; i--)
        {
            if (i == max_level)
            {
                create_initial_placement(G_mult_ptr.get(i), A_mult_ptr.get(i));
            }
            else
            {
                Mult.find_initial_placement_for_level(i, initialPlacementMult(), G_mult_ptr, A_mult_ptr, E_mult_ptr);
                update_boxlength_and_cornercoordinate(G_mult_ptr.get(i), A_mult_ptr.get(i));
            }
            call_FORCE_CALCULATION_step(G_mult_ptr.get(i), A_mult_ptr.get(i), E_mult_ptr.get(i),
                    i, max_level);
        }
        //Mult.delete_multilevel_representations(G_mult_ptr,A_mult_ptr,E_mult_ptr,max_level);
    }

    void call_FORCE_CALCULATION_step(
            Graph G,
            NodeArray<NodeAttributes> A,
            EdgeArray<EdgeAttributes> E,
            int act_level,
            int max_level)
    {
        int ITERBOUND = 10000;//needed to guarantee termination if
        //stopCriterion() == scThreshold
        if (G.numberOfNodes() > 1)
        {
            int iter = 1;
            int max_mult_iter = get_max_mult_iter(act_level, max_level, G.numberOfNodes());
            double actforcevectorlength = threshold() + 1;

            NodeArray<DPoint> F_rep = new NodeArray<DPoint>(G); //stores rep. forces
            NodeArray<DPoint> F_attr = new NodeArray<DPoint>(G); //stores attr. forces
            NodeArray<DPoint> F = new NodeArray<DPoint>(G); //stores resulting forces
            NodeArray<DPoint> last_node_movement = new NodeArray<DPoint>(G);//stores the force vectors F of the last
            //iterations (needed to avoid oscillations)

            set_average_ideal_edgelength(G, E);//needed for easy scaling of the forces
            make_initialisations_for_rep_calc_classes(G);

            while (((stopCriterion() == StopCriterion.scFixedIterations) && (iter <= max_mult_iter)) ||
                    ((stopCriterion() == StopCriterion.scThreshold) && (actforcevectorlength >= threshold()) &&
                    (iter <= ITERBOUND)) ||
                    ((stopCriterion() == StopCriterion.scFixedIterationsOrThreshold) && (iter <= max_mult_iter) &&
                    (actforcevectorlength >= threshold())))
            {//while
                calculate_forces(G, A, E, F, F_attr, F_rep, last_node_movement, iter, 0);
                if (stopCriterion() != StopCriterion.scFixedIterations)
                {
                    actforcevectorlength = get_average_forcevector_length(G, F);
                }
                iter++;
            }//while

            if (act_level == 0)
            {
                call_POSTPROCESSING_step(G, A, E, F, F_attr, F_rep, last_node_movement);
            }

            //deallocate_memory_for_rep_calc_classes();
        }
    }

    void call_POSTPROCESSING_step(
            Graph G,
            NodeArray<NodeAttributes> A,
            EdgeArray<EdgeAttributes> E,
            NodeArray<DPoint> F,
            NodeArray<DPoint> F_attr,
            NodeArray<DPoint> F_rep,
            NodeArray<DPoint> last_node_movement)
    {
        for (int i = 1; i <= 10; i++)
        {
            calculate_forces(G, A, E, F, F_attr, F_rep, last_node_movement, i, 1);
        }

        if ((resizeDrawing() == true))
        {
            adapt_drawing_to_ideal_average_edgelength(G, A, E);
            update_boxlength_and_cornercoordinate(G, A);
        }

        for (int i = 1; i <= fineTuningIterations(); i++)
        {
            calculate_forces(G, A, E, F, F_attr, F_rep, last_node_movement, i, 2);
        }

        if ((resizeDrawing() == true))
        {
            adapt_drawing_to_ideal_average_edgelength(G, A, E);
        }
    }

//------------------------- functions for pre/post-processing -------------------------
    void initialize_all_options()
    {
        //setting high level options
        useHighLevelOptions(false);
        pageFormat(PageFormatType.pfSquare);
        unitEdgeLength(100);
        newInitialPlacement(false);
        qualityVersusSpeed(QualityVsSpeed.qvsBeautifulAndFast);

        //setting low level options
        //setting general options
        randSeed(100);
        edgeLengthMeasurement(EdgeLengthMeasurement.elmBoundingCircle);
        allowedPositions(AllowedPositions.apInteger);
        maxIntPosExponent(40);

        //setting options for the divide et impera step
        pageRatio(1.0);
        stepsForRotatingComponents(10);
        tipOverCCs(TipOver.toNoGrowingRow);
        minDistCC(100);
        presortCCs(PreSort.psDecreasingHeight);

        //setting options for the multilevel step
        minGraphSize(50);
        galaxyChoice(GalaxyChoice.gcNonUniformProbLowerMass);
        randomTries(20);
        maxIterChange(MaxIterChange.micLinearlyDecreasing);
        maxIterFactor(10);
        initialPlacementMult(InitialPlacementMult.ipmAdvanced);
        m_singleLevel = false;

        //setting options for the force calculation step
        forceModel(ForceModel.fmNew);
        springStrength(1);
        repForcesStrength(1);
        repulsiveForcesCalculation(RepulsiveForcesMethod.rfcNMM);
        stopCriterion(StopCriterion.scFixedIterationsOrThreshold);
        threshold(0.01);
        fixedIterations(30);
        forceScalingFactor(0.05);
        coolTemperature(false);
        coolValue(0.99);
        initialPlacementForces(InitialPlacementForces.ipfRandomRandIterNr);

        //setting options for postprocessing
        resizeDrawing(true);
        resizingScalar(1);
        fineTuningIterations(20);
        fineTuneScalar(0.2);
        adjustPostRepStrengthDynamically(true);
        postSpringStrength(2.0);
        postStrengthOfRepForces(0.01);

        //setting options for different repulsive force calculation methods
        frGridQuotient(2);
        nmTreeConstruction(ReducedTreeConstruction.rtcSubtreeBySubtree);
        nmSmallCell(SmallestCellFinding.scfIteratively);
        nmParticlesInLeaves(25);
        nmPrecision(4);
    }

    void update_low_level_options_due_to_high_level_options_settings()
    {
        PageFormatType pf = pageFormat();
        double uel = unitEdgeLength();
        boolean nip = newInitialPlacement();
        QualityVsSpeed qvs = qualityVersusSpeed();

        //update
        initialize_all_options();
        useHighLevelOptions(true);
        pageFormat(pf);
        unitEdgeLength(uel);
        newInitialPlacement(nip);
        qualityVersusSpeed(qvs);

        if (pageFormat() == PageFormatType.pfSquare)
        {
            pageRatio(1.0);
        }
        else if (pageFormat() == PageFormatType.pfLandscape)
        {
            pageRatio(1.4142);
        }
        else //pageFormat() == pfPortrait
        {
            pageRatio(0.7071);
        }

        if (newInitialPlacement())
        {
            initialPlacementForces(InitialPlacementForces.ipfRandomTime);
        }
        else
        {
            initialPlacementForces(InitialPlacementForces.ipfRandomRandIterNr);
        }

        if (qualityVersusSpeed() == QualityVsSpeed.qvsGorgeousAndEfficient)
        {
            fixedIterations(60);
            fineTuningIterations(40);
            nmPrecision(6);
        }
        else if (qualityVersusSpeed() == QualityVsSpeed.qvsBeautifulAndFast)
        {
            fixedIterations(30);
            fineTuningIterations(20);
            nmPrecision(4);
        }
        else //qualityVersusSpeed() == qvsNiceAndIncredibleSpeed
        {
            fixedIterations(15);
            fineTuningIterations(10);
            nmPrecision(2);
        }
    }

    void import_NodeAttributes(
            Graph G,
            GraphAttributes GA,
            NodeArray<NodeAttributes> A)
    {
        DPoint position = new DPoint();

        for (Iterator<node> i = G.nodesIterator(); i.hasNext();)
        {
            node v = i.next();
            position.m_x = GA.x(v);
            position.m_y = GA.y(v);
            A.get(v).set_NodeAttributes(GA.width(v), GA.height(v), position, null, null);
        }
    }

    void import_EdgeAttributes(
            Graph G,
            EdgeArray<Double> edgeLength,
            EdgeArray<EdgeAttributes> E)
    {
        double length;

        for (Iterator<edge> i = G.edgesIterator(); i.hasNext();)
        {
            edge e = i.next();
            if (edgeLength.get(e) > 0) //no negative edgelength allowed
            {
                length = edgeLength.get(e);
            }
            else
            {
                length = 1;
            }

            E.get(e).set_EdgeAttributes(length, null, null);
        }
    }

    void init_ind_ideal_edgelength(
            Graph G,
            NodeArray<NodeAttributes> A,
            EdgeArray<EdgeAttributes> E)
    {
        if (edgeLengthMeasurement() == EdgeLengthMeasurement.elmMidpoint)
        {
            for (Iterator<edge> i = G.edgesIterator(); i.hasNext();)
            {
                edge e = i.next();
                E.get(e).set_length(E.get(e).get_length() * unitEdgeLength());
            }
        }
        else //(edgeLengthMeasurement() == elmBoundingCircle)
        {
            set_radii(G, A);
            for (Iterator<edge> i = G.edgesIterator(); i.hasNext();)
            {
                edge e = i.next();
                E.get(e).set_length(E.get(e).get_length() * unitEdgeLength() + radius.get(e.source()) +
                        radius.get(e.target()));
            }
        }
    }

    void set_radii(Graph G, NodeArray<NodeAttributes> A)
    {
        radius.init(G);
        double w, h;
        for (Iterator<node> i = G.nodesIterator(); i.hasNext();)
        {
            node v = i.next();
            w = A.get(v).get_width() / 2;
            h = A.get(v).get_height() / 2;
            radius.set(v, new Double(Math.sqrt(w * w + h * h)));
        }
    }

    void export_NodeAttributes(
            Graph G_reduced,
            NodeArray<NodeAttributes> A_reduced,
            GraphAttributes GA)
    {
        for (Iterator<node> i = G_reduced.nodesIterator(); i.hasNext();)
        {
            node v_copy = i.next();

            GA.setX(A_reduced.get(v_copy).get_original_node(), A_reduced.get(v_copy).get_position().m_x);
            GA.setY(A_reduced.get(v_copy).get_original_node(), A_reduced.get(v_copy).get_position().m_y);
        }
    }

    void make_simple_loopfree(
            Graph G,
            NodeArray<NodeAttributes> A,
            EdgeArray<EdgeAttributes> E,
            Graph G_reduced,
            NodeArray<NodeAttributes> A_reduced,
            EdgeArray<EdgeAttributes> E_reduced)
    {
        node u_orig, v_orig, v_reduced;
        edge e_reduced, e_orig;

        //create the reduced Graph G_reduced and save in A/E links to node/edges of G_reduced
        //create G_reduced as a copy of G without selfloops!

        G_reduced.clear();
        for (Iterator<node> i = G.nodesIterator(); i.hasNext();)
        {
            v_orig = i.next();
            A.get(v_orig).set_copy_node(G_reduced.newNode());
        }

        for (Iterator<edge> i = G.edgesIterator(); i.hasNext();)
        {
            e_orig = i.next();
            u_orig = e_orig.source();
            v_orig = e_orig.target();
            if (u_orig != v_orig)
            {
                E.get(e_orig).set_copy_edge(G_reduced.newEdge(A.get(u_orig).get_copy_node(),
                        A.get(v_orig).get_copy_node()));
            }
            else
            {
                E.get(e_orig).set_copy_edge(null);//mark this edge as deleted
            }
        }

        //remove parallel (and reversed) edges from G_reduced
        EdgeArray<Double> new_edgelength = new EdgeArray<Double>(G_reduced);
        List<edge> S = new ArrayList<edge>();
        S.clear();
        delete_parallel_edges(G, E, G_reduced, S, new_edgelength);

        //make A_reduced, E_reduced valid for G_reduced
        A_reduced.init(G_reduced);
        E_reduced.init(G_reduced);

        //import information for A_reduced, E_reduced and links to the original nodes/edges
        //of the copy nodes/edges
        for (Iterator<node> i = G.nodesIterator(); i.hasNext();)
        {
            v_orig = i.next();
            v_reduced = A.get(v_orig).get_copy_node();
            A_reduced.get(v_reduced).set_NodeAttributes(A.get(v_orig).get_width(), A.get(v_orig).
                    get_height(), A.get(v_orig).get_position(),
                    v_orig, null);
        }
        for (Iterator<edge> i = G.edgesIterator(); i.hasNext();)
        {
            e_orig = i.next();
            e_reduced = E.get(e_orig).get_copy_edge();
            if (e_reduced != null)
            {
                E_reduced.get(e_reduced).set_EdgeAttributes(E.get(e_orig).get_length(), e_orig, null);
            }
        }

        //update edgelength of copy edges in G_reduced associated with a set of parallel
        //edges in G
        update_edgelength(S, new_edgelength, E_reduced);
    }

    void delete_parallel_edges(
            Graph G,
            EdgeArray<EdgeAttributes> E,
            Graph G_reduced,
            List<edge> S,
            EdgeArray<Double> new_edgelength)
    {
        ListIterator<Edge> EdgeIterator;
        edge e_act, e_save = null;
        Edge f_act = new Edge();
        List<Edge> sorted_edges = new ArrayList<Edge>();
        EdgeArray<edge> original_edge = new EdgeArray<edge>(G_reduced); //helping array
	int save_s_index = 0, save_t_index = 0, act_s_index = 0, act_t_index = 0;
        int counter = 1;
        Graph Graph_ptr = G_reduced;

        //save the original edges for each edge in G_reduced
        for (Iterator<edge> i = G.edgesIterator(); i.hasNext();)
        {
            e_act = i.next();
            if (E.get(e_act).get_copy_edge() != null) //e_act is no self_loops
            {
                original_edge.set(E.get(e_act).get_copy_edge(), e_act);
            }
        }

        for (Iterator<edge> i = G_reduced.edgesIterator(); i.hasNext();)
        {
            e_act = i.next();
            f_act.set_Edge(e_act, Graph_ptr);
            sorted_edges.add(f_act);
        }

        // FIXME: may not be correct
        Collections.sort(sorted_edges, new java.util.Comparator<Edge>()
        {
            @Override
            public int compare(Edge a, Edge b)
            {
		int a_index = a.get_edge().source().index() -
                    a.get_edge().target().index();
                int b_index = b.get_edge().source().index() -
                    b.get_edge().target().index();

                return b_index - a_index;
            }
        });

        //now parallel edges are consecutive in sorted_edges
        for (Edge e : sorted_edges)
        {//for
            e_act = e.get_edge();
            act_s_index = e_act.source().index();
            act_t_index = e_act.target().index();

            if (e != sorted_edges.get(0))
            {//if
                if ((act_s_index == save_s_index && act_t_index == save_t_index) ||
                        (act_s_index == save_t_index && act_t_index == save_s_index))
                {
                    if (counter == 1) //first parallel edge
                    {
                        S.add(e_save);
                        new_edgelength.set(e_save, E.get(original_edge.get(e_save)).get_length() +
                                E.get(original_edge.get(e_act)).get_length());
                    }
                    else //more then two parallel edges
                    {
                        new_edgelength.set(e_save, new_edgelength.get(e_save) +
                                E.get(original_edge.get(e_act)).get_length());
                    }

                    E.get(original_edge.get(e_act)).set_copy_edge(null); //mark copy of edge as deleted
                    G_reduced.delEdge(e_act);                    //delete copy edge in G_reduced
                    counter++;
                }
                else
                {
                    if (counter > 1)
                    {
                        new_edgelength.set(e_save, new_edgelength.get(e_save) / counter);
                        counter = 1;
                    }
                    save_s_index = act_s_index;
                    save_t_index = act_t_index;
                    e_save = e_act;
                }
            }//if
            else //first edge
            {
                save_s_index = act_s_index;
                save_t_index = act_t_index;
                e_save = e_act;
            }
        }//for

        //treat special case (last edges were multiple edges)
        if (counter > 1)
        {
            new_edgelength.set(e_save, new_edgelength.get(e_save) / counter);
        }
    }

    void update_edgelength(
            List<edge> S,
            EdgeArray<Double> new_edgelength,
            EdgeArray<EdgeAttributes> E_reduced)
    {
        edge e;
        while (!S.isEmpty())
        {
            e = S.get(0);
            S.remove(0); //S.popFrontRet();
            E_reduced.get(e).set_length(new_edgelength.get(e));
        }
    }

    double get_post_rep_force_strength(int n)
    {
        return Math.min(0.2, 400.0 / (double) n);
    }

    void make_positions_integer(Graph G, NodeArray<NodeAttributes> A)
    {
        node v;
        double new_x, new_y;

        if (allowedPositions() == AllowedPositions.apInteger)
        {//if
            //calculate value of max_integer_position
            max_integer_position = 100 * average_ideal_edgelength * G.numberOfNodes() *
                    G.numberOfNodes();
        }//if

        //restrict positions to lie in [-max_integer_position,max_integer_position]
        //X [-max_integer_position,max_integer_position]
        for (Iterator<node> i = G.nodesIterator(); i.hasNext();)
        {
            v = i.next();
            if ((A.get(v).get_x() > max_integer_position) ||
                    (A.get(v).get_y() > max_integer_position) ||
                    (A.get(v).get_x() < max_integer_position * (-1.0)) ||
                    (A.get(v).get_y() < max_integer_position * (-1.0)))
            {
                DPoint cross_point = new DPoint();
                DPoint nullpoint = new DPoint(0, 0);
                DPoint old_pos = new DPoint(A.get(v).get_x(), A.get(v).get_y());
                DPoint lt = new DPoint(max_integer_position * (-1.0), max_integer_position);
                DPoint rt = new DPoint(max_integer_position, max_integer_position);
                DPoint lb = new DPoint(max_integer_position * (-1.0), max_integer_position * (-1.0));
                DPoint rb = new DPoint(max_integer_position, max_integer_position * (-1.0));
                DLine s = new DLine(nullpoint, old_pos);
                DLine left_bound = new DLine(lb, lt);
                DLine right_bound = new DLine(rb, rt);
                DLine top_bound = new DLine(lt, rt);
                DLine bottom_bound = new DLine(lb, rb);

                if (s.intersection(left_bound, cross_point))
                {
                    A.get(v).set_x(cross_point.m_x);
                    A.get(v).set_y(cross_point.m_y);
                }
                else if (s.intersection(right_bound, cross_point))
                {
                    A.get(v).set_x(cross_point.m_x);
                    A.get(v).set_y(cross_point.m_y);
                }
                else if (s.intersection(top_bound, cross_point))
                {
                    A.get(v).set_x(cross_point.m_x);
                    A.get(v).set_y(cross_point.m_y);
                }
                else if (s.intersection(bottom_bound, cross_point))
                {
                    A.get(v).set_x(cross_point.m_x);
                    A.get(v).set_y(cross_point.m_y);
                }
                else
                {
                    System.out.println("Error  make_positions_integer()");
                }
            }
        }

        //make positions integer
        for (Iterator<node> i = G.nodesIterator(); i.hasNext();)
        {
            v = i.next();
            new_x = Math.floor(A.get(v).get_x());
            new_y = Math.floor(A.get(v).get_y());
            if (new_x < down_left_corner.m_x)
            {
                boxlength += 2;
                down_left_corner.m_x = down_left_corner.m_x - 2;
            }
            if (new_y < down_left_corner.m_y)
            {
                boxlength += 2;
                down_left_corner.m_y = down_left_corner.m_y - 2;
            }
            A.get(v).set_x(new_x);
            A.get(v).set_y(new_y);
        }
    }


    /*void create_postscript_drawing(GraphAttributes& AG, char* ps_file)
     {
     ofstream out_fmmm (ps_file,ios::out);
     if (!ps_file) cout<<ps_file<<" could not be opened !"<<endl;
     Graph G = AG.constGraph();
     node v;
     edge e;
     double x_min = AG.x(G.firstNode());
     double x_max = x_min;
     double y_min = AG.y(G.firstNode());
     double y_max = y_min;
     double max_dist;
     double scale_factor;

     forall_nodes(v,G)
     {
     if(AG.x(v) < x_min)
     x_min = AG.x(v);
     else if(AG.x(v) > x_max)
     x_max = AG.x(v);
     if(AG.y(v) < y_min)
     y_min = AG.y(v);
     else if(AG.y(v) > y_max)
     y_max = AG.y(v);
     }
     max_dist = max(x_max -x_min,y_max-y_min);
     scale_factor = 500.0/max_dist;

     out_fmmm<<"%!PS-Adobe-2.0 "<<endl;
     out_fmmm<<"%%Pages:  1 "<<endl;
     out_fmmm<<"% %BoundingBox: "<<x_min<<" "<<x_max<<" "<<y_min<<" "<<y_max<<endl;
     out_fmmm<<"%%EndComments "<<endl;
     out_fmmm<<"%%"<<endl;
     out_fmmm<<"%% Circle"<<endl;
     out_fmmm<<"/ellipse_dict 4 dict def"<<endl;
     out_fmmm<<"/ellipse {"<<endl;
     out_fmmm<<"  ellipse_dict"<<endl;
     out_fmmm<<"  begin"<<endl;
     out_fmmm<<"   newpath"<<endl;
     out_fmmm<<"   /yrad exch def /xrad exch def /ypos exch def /xpos exch def"<<endl;
     out_fmmm<<"   matrix currentmatrix"<<endl;
     out_fmmm<<"   xpos ypos translate"<<endl;
     out_fmmm<<"   xrad yrad scale"<<endl;
     out_fmmm<<"  0 0 1 0 360 arc"<<endl;
     out_fmmm<<"  setmatrix"<<endl;
     out_fmmm<<"  closepath"<<endl;
     out_fmmm<<" end"<<endl;
     out_fmmm<<"} def"<<endl;
     out_fmmm<<"%% Nodes"<<endl;
     out_fmmm<<"/v { "<<endl;
     out_fmmm<<" /y exch def"<<endl;
     out_fmmm<<" /x exch def"<<endl;
     out_fmmm<<"1.000 1.000 0.894 setrgbcolor"<<endl;
     out_fmmm<<"x y 10.0 10.0 ellipse fill"<<endl;
     out_fmmm<<"0.000 0.000 0.000 setrgbcolor"<<endl;
     out_fmmm<<"x y 10.0 10.0 ellipse stroke"<<endl;
     out_fmmm<<"} def"<<endl;
     out_fmmm<<"%% Edges"<<endl;
     out_fmmm<<"/e { "<<endl;
     out_fmmm<<" /b exch def"<<endl;
     out_fmmm<<" /a exch def"<<endl;
     out_fmmm<<" /y exch def"<<endl;
     out_fmmm<<" /x exch def"<<endl;
     out_fmmm<<"x y moveto a b lineto stroke"<<endl;
     out_fmmm<<"} def"<<endl;
     out_fmmm<<"%% "<<endl;
     out_fmmm<<"%% INIT "<<endl;
     out_fmmm<<"20  200 translate"<<endl;
     out_fmmm<<scale_factor<<"  "<<scale_factor<<"  scale "<<endl;
     out_fmmm<<"1 setlinewidth "<<endl;
     out_fmmm<<"%%BeginProgram "<<endl;
     forall_edges(e,G)
     out_fmmm<<AG.x(e->source())<<" "<<AG.y(e->source())<<" "
     <<AG.x(e->target())<<" "<<AG.y(e->target())<<" e"<<endl;
     forall_nodes(v,G)
     out_fmmm<<AG.x(v)<<" "<<AG.y(v) <<" v"<<endl;
     out_fmmm<<"%%EndProgram "<<endl;
     out_fmmm<<"showpage "<<endl;
     out_fmmm<<"%%EOF "<<endl;
     }*/
//------------------------- functions for divide et impera step -----------------------
    void create_maximum_connected_subGraphs(
            Graph G,
            NodeArray<NodeAttributes> A,
            EdgeArray<EdgeAttributes> E,
            List<Graph> G_sub,
            List<NodeArray<NodeAttributes>> A_sub,
            List<EdgeArray<EdgeAttributes>> E_sub,
            NodeArray<Integer> component)
    {
        node u_orig, v_orig, v_sub;
        edge e_sub, e_orig;
        int i;

        //create the subgraphs and save links to subgraph nodes/edges in A
        for (Iterator<node> iter = G.nodesIterator(); iter.hasNext();)
        {
            v_orig = iter.next();
            A.get(v_orig).set_subgraph_node(G_sub.get(component.get(v_orig)).newNode());
        }

        for (Iterator<edge> iter = G.edgesIterator(); iter.hasNext();)
        {
            e_orig = iter.next();
            u_orig = e_orig.source();
            v_orig = e_orig.target();
            E.get(e_orig).set_subgraph_edge(G_sub.get(component.get(u_orig)).newEdge(A.get(u_orig).get_subgraph_node(), A.get(v_orig).get_subgraph_node()));
        }

        //make A_sub,E_sub valid for the subgraphs
        for (i = 0; i < number_of_components; i++)
        {
            A_sub.get(i).init(G_sub.get(i));
            E_sub.get(i).init(G_sub.get(i));
        }

        //import information for A_sub,E_sub and links to the original nodes/edges
        //of the subGraph nodes/edges

        for (Iterator<node> iter = G.nodesIterator(); iter.hasNext();)
        {
            v_orig = iter.next();
            v_sub = A.get(v_orig).get_subgraph_node();
            A_sub.get(component.get(v_orig)).get(v_sub).set_NodeAttributes(A.get(v_orig).get_width(),
                    A.get(v_orig).get_height(), A.get(v_orig).get_position(),
                    v_orig, null);
        }

        for (Iterator<edge> iter = G.edgesIterator(); iter.hasNext();)
        {
            e_orig = iter.next();
            e_sub = E.get(e_orig).get_subgraph_edge();
            v_orig = e_orig.source();
            E_sub.get(component.get(v_orig)).get(e_sub).set_EdgeAttributes(E.get(e_orig).get_length(),
                    e_orig, null);
        }
    }

    void pack_subGraph_drawings(
            NodeArray<NodeAttributes> A,
            List<Graph> G_sub,
            List<NodeArray<NodeAttributes>> A_sub)
    {
        //double aspect_ratio_area, bounding_rectangles_area;
        MAARPacking P;
        List<Rectangle> R = new ArrayList<Rectangle>();

        if (stepsForRotatingComponents() == 0) //no rotation
        {
            calculate_bounding_rectangles_of_components(R, G_sub, A_sub);
        }
        else
        {
            rotate_components_and_calculate_bounding_rectangles(R, G_sub, A_sub);
        }

        P.pack_rectangles_using_Best_Fit_strategy(R, pageRatio(), presortCCs(),
                tipOverCCs());/*, aspect_ratio_area,
                bounding_rectangles_area);*/
        export_node_positions(A, R, G_sub, A_sub);
    }

    void calculate_bounding_rectangles_of_components(
            List<Rectangle> R,
            List<Graph> G_sub,
            List<NodeArray<NodeAttributes>> A_sub)
    {
        int i;
        Rectangle r;
        R.clear();

        for (i = 0; i < number_of_components; i++)
        {
            r = calculate_bounding_rectangle(G_sub.get(i), A_sub.get(i), i);
            R.add(r);
        }
    }

    Rectangle calculate_bounding_rectangle(
            Graph G,
            NodeArray<NodeAttributes> A,
            int componenet_index)
    {
        Rectangle r = new Rectangle();
        node v;
        double x_min = Double.MAX_VALUE,
                x_max = Double.MIN_VALUE,
                y_min = Double.MAX_VALUE,
                y_max = Double.MIN_VALUE,
                act_x_min, act_x_max, act_y_min, act_y_max;
        double max_boundary;//the maximum of half of the width and half of the height of
        //each node; (needed to be able to tipp rectangles over without
        //having access to the height and width of each node)

        for (Iterator<node> iter = G.nodesIterator(); iter.hasNext();)
        {
            v = iter.next();
            max_boundary = Math.max(A.get(v).get_width() / 2, A.get(v).get_height() / 2);
            if (v == G.firstNode())
            {
                x_min = A.get(v).get_x() - max_boundary;
                x_max = A.get(v).get_x() + max_boundary;
                y_min = A.get(v).get_y() - max_boundary;
                y_max = A.get(v).get_y() + max_boundary;
            }
            else
            {
                act_x_min = A.get(v).get_x() - max_boundary;
                act_x_max = A.get(v).get_x() + max_boundary;
                act_y_min = A.get(v).get_y() - max_boundary;
                act_y_max = A.get(v).get_y() + max_boundary;
                if (act_x_min < x_min)
                {
                    x_min = act_x_min;
                }
                if (act_x_max > x_max)
                {
                    x_max = act_x_max;
                }
                if (act_y_min < y_min)
                {
                    y_min = act_y_min;
                }
                if (act_y_max > y_max)
                {
                    y_max = act_y_max;
                }
            }
        }

        //add offset
        x_min -= minDistCC() / 2;
        x_max += minDistCC() / 2;
        y_min -= minDistCC() / 2;
        y_max += minDistCC() / 2;

        r.set_rectangle(x_max - x_min, y_max - y_min, x_min, y_min, componenet_index);
        return r;
    }

    void rotate_components_and_calculate_bounding_rectangles(
            List<Rectangle> R,
            List<Graph> G_sub,
            List<NodeArray<NodeAttributes>> A_sub)
    {
        int i, j;
        double sin_j, cos_j;
        double angle, act_area, act_area_PI_half_rotated = 0.0, best_area;
        double ratio, new_width, new_height;
        List<NodeArray<DPoint>> best_coords = new ArrayList<NodeArray<DPoint>>(number_of_components); //FIXME needs init
        List<NodeArray<DPoint>> old_coords = new ArrayList<NodeArray<DPoint>>(number_of_components); //FIXME needs init
        node v_sub;
        Rectangle r_act, r_best;
        DPoint new_pos = new DPoint(), new_dlc = new DPoint();

        R.clear(); //make R empty

        for (i = 0; i < number_of_components; i++)
        {//allcomponents

            //init r_best, best_area and best_(old)coords
            r_best = calculate_bounding_rectangle(G_sub.get(i), A_sub.get(i), i);
            best_area = calculate_area(r_best.get_width(), r_best.get_height(),
                    number_of_components);
            best_coords.get(i).init(G_sub.get(i));
            old_coords.get(i).init(G_sub.get(i));

            for (Iterator<node> iter = G_sub.get(i).nodesIterator(); iter.hasNext();)
            {
                v_sub = iter.next();
                DPoint p = A_sub.get(i).get(v_sub).get_position();
                old_coords.get(i).set(v_sub, p);
                best_coords.get(i).set(v_sub, p);

            }

            //rotate the components
            for (j = 1; j <= stepsForRotatingComponents(); j++)
            {
                //calculate new positions for the nodes, the new rectangle and area
                angle = (Math.PI * 0.5) * (double) j / (double) (stepsForRotatingComponents() + 1);
                sin_j = Math.sin(angle);
                cos_j = Math.cos(angle);
                for (Iterator<node> iter = G_sub.get(i).nodesIterator(); iter.hasNext();)
                {
                    v_sub = iter.next();
                    new_pos.m_x = cos_j * old_coords.get(i).get(v_sub).m_x -
                            sin_j * old_coords.get(i).get(v_sub).m_y;
                    new_pos.m_y = sin_j * old_coords.get(i).get(v_sub).m_x +
                            cos_j * old_coords.get(i).get(v_sub).m_y;
                    A_sub.get(i).get(v_sub).set_position(new_pos);
                }

                r_act = calculate_bounding_rectangle(G_sub.get(i), A_sub.get(i), i);
                act_area = calculate_area(r_act.get_width(), r_act.get_height(),
                        number_of_components);
                if (number_of_components == 1)
                {
                    act_area_PI_half_rotated = calculate_area(r_act.get_height(),
                            r_act.get_width(),
                            number_of_components);
                }

                //store placement of the nodes with minimal area (in case that
                //number_of_components >1) else store placement with minimal aspect ratio area
                if (act_area < best_area)
                {
                    r_best = r_act;
                    best_area = act_area;
                    for (Iterator<node> iter = G_sub.get(i).nodesIterator(); iter.hasNext();)
                    {
                        v_sub = iter.next();

                        best_coords.get(i).set(v_sub, A_sub.get(i).get(v_sub).get_position());
                    }
                }
                else if ((number_of_components == 1) && (act_area_PI_half_rotated < best_area))
                { //test if rotating further with PI_half would be an improvement
                    r_best = r_act;
                    best_area = act_area_PI_half_rotated;
                    for (Iterator<node> iter = G_sub.get(i).nodesIterator(); iter.hasNext();)
                    {
                        v_sub = iter.next();

                        best_coords.get(i).set(v_sub, A_sub.get(i).get(v_sub).get_position());
                    }
                    //the needed rotation step follows in the next if statement
                }
            }

            //tipp the smallest rectangle over by angle PI/2 around the origin if it makes the
            //aspect_ratio of r_best more similar to the desired aspect_ratio
            ratio = r_best.get_width() / r_best.get_height();

            if ((pageRatio() < 1 && ratio > 1) || (pageRatio() >= 1 && ratio < 1))
            {
                for (Iterator<node> iter = G_sub.get(i).nodesIterator(); iter.hasNext();)
                {
                    v_sub = iter.next();
                    new_pos.m_x = best_coords.get(i).get(v_sub).m_y * (-1);
                    new_pos.m_y = best_coords.get(i).get(v_sub).m_x;
                    best_coords.get(i).set(v_sub, new_pos);
                }

                //calculate new rectangle
                new_dlc.m_x = r_best.get_old_dlc_position().m_y * (-1) - r_best.get_height();
                new_dlc.m_y = r_best.get_old_dlc_position().m_x;
                new_width = r_best.get_height();
                new_height = r_best.get_width();
                r_best.set_width(new_width);
                r_best.set_height(new_height);
                r_best.set_old_dlc_position(new_dlc);
            }

            //save the computed information in A_sub and R
            for (Iterator<node> iter = G_sub.get(i).nodesIterator(); iter.hasNext();)
            {
                v_sub = iter.next();
                A_sub.get(i).get(v_sub).set_position(best_coords.get(i).get(v_sub));
            }
            R.add(r_best);

        }//allcomponents
    }

    double calculate_area(double width, double height, int comp_nr)
    {
        if (comp_nr == 1)  //calculate aspect ratio area of the rectangle
        {
            double ratio = width / height;

            if (ratio < pageRatio()) //scale width
            {
                return (width * height * (pageRatio() / ratio));
            }
            else //scale height
            {
                return (width * height * (ratio / pageRatio()));
            }
        }
        else  //calculate area of the rectangle
        {
            return width * height;
        }
    }

    void export_node_positions(
            NodeArray<NodeAttributes> A,
            List<Rectangle> R,
            List<Graph> G_sub,
            List<NodeArray<NodeAttributes>> A_sub)
    {
        ListIterator<Rectangle> RectIterator;
        int i;
        node v_sub;
        DPoint newpos, tipped_pos = new DPoint(), tipped_dlc;

        for (Rectangle r : R)
        {//for
            i = r.get_component_index();
            if (r.is_tipped_over())
            {//if
                //calculate tipped coordinates of the nodes
                for (Iterator<node> iter = G_sub.get(i).nodesIterator(); iter.hasNext();)
                {
                    v_sub = iter.next();
                    tipped_pos.m_x = A_sub.get(i).get(v_sub).get_y() * (-1);
                    tipped_pos.m_y = A_sub.get(i).get(v_sub).get_x();
                    A_sub.get(i).get(v_sub).set_position(tipped_pos);
                }
            }//if

            for (Iterator<node> iter = G_sub.get(i).nodesIterator(); iter.hasNext();)
            {
                v_sub = iter.next();
                newpos = A_sub.get(i).get(v_sub).get_position().plus(r.get_new_dlc_position()).minus(
                        r.get_old_dlc_position());
                A.get(A_sub.get(i).get(v_sub).get_original_node()).set_position(newpos);
            }
        }//for
    }

//----------------------- functions for multilevel step -----------------------------
    int get_max_mult_iter(int act_level, int max_level, int node_nr)
    {
        int iter;
        if (maxIterChange() == MaxIterChange.micConstant) //nothing to do
        {
            iter = fixedIterations();
        }
        else if (maxIterChange() == MaxIterChange.micLinearlyDecreasing) //linearly decreasing values
        {
            if (max_level == 0)
            {
                iter = fixedIterations() + ((maxIterFactor() - 1) * fixedIterations());
            }
            else
            {
                iter = fixedIterations() + (int) ((double) act_level / (double) max_level) *
                        (((maxIterFactor() - 1)) * fixedIterations());
            }
        }
        else //maxIterChange == micRapidlyDecreasing (rapidly decreasing values)
        {
            if (act_level == max_level)
            {
                iter = fixedIterations() + (int) ((maxIterFactor() - 1) * fixedIterations());
            }
            else if (act_level == max_level - 1)
            {
                iter = fixedIterations() + (int) (0.5 * (maxIterFactor() - 1) * fixedIterations());
            }
            else if (act_level == max_level - 2)
            {
                iter = fixedIterations() + (int) (0.25 * (maxIterFactor() - 1) * fixedIterations());
            }
            else //act_level >= max_level - 3
            {
                iter = fixedIterations();
            }
        }

        //helps to get good drawings for small graphs and graphs with few multilevels
        if ((node_nr <= 500) && (iter < 100))
        {
            return 100;
        }
        else
        {
            return iter;
        }
    }

//-------------------------- functions for force calculation ---------------------------
    void calculate_forces(
            Graph G,
            NodeArray<NodeAttributes> A,
            EdgeArray<EdgeAttributes> E,
            NodeArray<DPoint> F,
            NodeArray<DPoint> F_attr,
            NodeArray<DPoint> F_rep,
            NodeArray<DPoint> last_node_movement,
            int iter,
            int fine_tuning_step)
    {
        if (allowedPositions() != AllowedPositions.apAll)
        {
            make_positions_integer(G, A);
        }
        calculate_attractive_forces(G, A, E, F_attr);
        calculate_repulsive_forces(G, A, F_rep);
        add_attr_rep_forces(G, F_attr, F_rep, F, iter, fine_tuning_step);
        prevent_oscilations(G, F, last_node_movement, iter);
        move_nodes(G, A, F);
        update_boxlength_and_cornercoordinate(G, A);
    }

    void init_boxlength_and_cornercoordinate(
            Graph G,
            NodeArray<NodeAttributes> A)
    {
        //boxlength is set

        double MIN_NODE_SIZE = 10;
        double BOX_SCALING_FACTOR = 1.1;
        double w = 0, h = 0;       //helping variables

        for (Iterator<node> iter = G.nodesIterator(); iter.hasNext();)
        {
            node v = iter.next();
            w += Math.max(A.get(v).get_width(), MIN_NODE_SIZE);
            h += Math.max(A.get(v).get_height(), MIN_NODE_SIZE);
        }

        boxlength = Math.ceil(Math.max(w, h) * BOX_SCALING_FACTOR);

        //down left corner of comp. box is the origin
        down_left_corner.m_x = 0;
        down_left_corner.m_y = 0;
    }

    void create_initial_placement(Graph G, NodeArray<NodeAttributes> A)
    {
        int BILLION = 1000000000;
        int i, j, k;
        node v;

        if (initialPlacementForces() == InitialPlacementForces.ipfKeepPositions) // don't change anything
        {
            init_boxlength_and_cornercoordinate(G, A);
        }
        else if (initialPlacementForces() == InitialPlacementForces.ipfUniformGrid) //set nodes to the midpoints of a  grid
        {//(uniform on a grid)
            init_boxlength_and_cornercoordinate(G, A);
            int level = (int) (Math.ceil(Math.log(G.numberOfNodes()) / Math.log(4.0)));
            int m = (int) (Math.pow(2.0, level)) - 1;
            boolean finished = false;
            double blall = boxlength / (m + 1); //boxlength for boxes at the lowest level (depth)
            List<node> all_nodes = new ArrayList<node>(G.numberOfNodes());

            for (Iterator<node> iter = G.nodesIterator(); iter.hasNext();)
            {
                v = iter.next();
                all_nodes.add(v);
            }
            v = all_nodes.get(0);
            k = 0;
            i = 0;
            while ((!finished) && (i <= m))
            {//while1
                j = 0;
                while ((!finished) && (j <= m))
                {//while2
                    A.get(v).set_x(boxlength * i / (m + 1) + blall / 2);
                    A.get(v).set_y(boxlength * j / (m + 1) + blall / 2);
                    if (k == G.numberOfNodes() - 1)
                    {
                        finished = true;
                    }
                    else
                    {
                        k++;
                        v = all_nodes.get(k);
                    }
                    j++;
                }//while2
                i++;
            }//while1
        }//(uniform on a grid)
        else //randomised distribution of the nodes;
        {//(random)
            Random random = new Random();
            init_boxlength_and_cornercoordinate(G, A);
            if (initialPlacementForces() == InitialPlacementForces.ipfRandomTime)//(RANDOM based on actual CPU-time)
            {
                random.setSeed(System.currentTimeMillis() / 1000);
            }
            else if (initialPlacementForces() == InitialPlacementForces.ipfRandomRandIterNr)//(RANDOM based on seed)
            {
                random.setSeed(randSeed());
            }

            for (Iterator<node> iter = G.nodesIterator(); iter.hasNext();)
            {
                v = iter.next();
                DPoint rndp = new DPoint();
                rndp.m_x = random.nextDouble();//rand_x in [0,1]
                rndp.m_y = random.nextDouble();//rand_y in [0,1]
                A.get(v).set_x(rndp.m_x * (boxlength - 2) + 1);
                A.get(v).set_y(rndp.m_y * (boxlength - 2) + 1);
            }
        }//(random)
        update_boxlength_and_cornercoordinate(G, A);
    }

    void init_F(Graph G, NodeArray<DPoint> F)
    {
        DPoint nullpoint = new DPoint(0, 0);

        for (Iterator<node> iter = G.nodesIterator(); iter.hasNext();)
        {
            node v = iter.next();
            F.set(v, nullpoint);
        }
    }

    void make_initialisations_for_rep_calc_classes(Graph G)
    {
        if (repulsiveForcesCalculation() == RepulsiveForcesMethod.rfcExact)
        {
            FR.make_initialisations(boxlength, down_left_corner, frGridQuotient());
        }
        else if (repulsiveForcesCalculation() == RepulsiveForcesMethod.rfcGridApproximation)
        {
            FR.make_initialisations(boxlength, down_left_corner, frGridQuotient());
        }
        else //(repulsiveForcesCalculation() == RepulsiveForcesCalculation.rfcNMM
        {
            NM.make_initialisations(G, boxlength, down_left_corner,
                    nmParticlesInLeaves(), nmPrecision(),
                    nmTreeConstruction(), nmSmallCell());
        }
    }

    //! Calculates repulsive forces for each node.
    void calculate_repulsive_forces(
            Graph G,
            NodeArray<NodeAttributes> A,
            NodeArray<DPoint> F_rep)
    {
        if (repulsiveForcesCalculation() == RepulsiveForcesMethod.rfcExact)
        {
            FR.calculate_exact_repulsive_forces(G, A, F_rep);
        }
        else if (repulsiveForcesCalculation() == RepulsiveForcesMethod.rfcGridApproximation)
        {
            FR.calculate_approx_repulsive_forces(G, A, F_rep);
        }
        else //repulsiveForcesCalculation() == rfcNMM
        {
            NM.calculate_repulsive_forces(G, A, F_rep);
        }
    }

    void calculate_attractive_forces(
            Graph G,
            NodeArray<NodeAttributes> A,
            EdgeArray<EdgeAttributes> E,
            NodeArray<DPoint> F_attr)
    {
        edge e;
        node u, v;
        double norm_v_minus_u, scalar;
        DPoint vector_v_minus_u, f_u = new DPoint();
        DPoint nullpoint = new DPoint(0, 0);

        //initialisation
        init_F(G, F_attr);

        //calculation
        for (Iterator<edge> i = G.edgesIterator(); i.hasNext();)
        {//for
            e = i.next();

            u = e.source();
            v = e.target();
            vector_v_minus_u = A.get(v).get_position().minus(A.get(u).get_position());
            norm_v_minus_u = vector_v_minus_u.norm();
            if (vector_v_minus_u == nullpoint)
            {
                f_u = nullpoint;
            }
            else if (!numexcept.f_near_machine_precision(norm_v_minus_u, f_u))
            {
                scalar = f_attr_scalar(norm_v_minus_u, E.get(e).get_length()) / norm_v_minus_u;
                f_u.m_x = scalar * vector_v_minus_u.m_x;
                f_u.m_y = scalar * vector_v_minus_u.m_y;
            }

            F_attr.set(v, F_attr.get(v).minus(f_u));
            F_attr.set(u, F_attr.get(u).plus(f_u));
        }//for
    }

    double f_attr_scalar(double d, double ind_ideal_edge_length)
    {
        double s = 0.0;

        if (forceModel() == ForceModel.fmFruchtermanReingold)
        {
            s = d * d / (ind_ideal_edge_length * ind_ideal_edge_length * ind_ideal_edge_length);
        }
        else if (forceModel() == ForceModel.fmEades)
        {
            double c = 10;
            if (d == 0)
            {
                s = -1e10;
            }
            else
            {
                s = c * (Math.log(d / ind_ideal_edge_length) / Math.log(2)) / (ind_ideal_edge_length);
            }
        }
        else if (forceModel() == ForceModel.fmNew)
        {
            double c = Math.log(d / ind_ideal_edge_length) / Math.log(2);
            if (d > 0)
            {
                s = c * d * d /
                        (ind_ideal_edge_length * ind_ideal_edge_length * ind_ideal_edge_length);
            }
            else
            {
                s = -1e10;
            }
        }
        else
        {
            System.out.println(" Error  f_attr_scalar");
        }

        return s;
    }

    void add_attr_rep_forces(
            Graph G,
            NodeArray<DPoint> F_attr,
            NodeArray<DPoint> F_rep,
            NodeArray<DPoint> F,
            int iter,
            int fine_tuning_step)
    {
        node v;
        DPoint f = new DPoint(), force = new DPoint();
        DPoint nullpoint = new DPoint(0, 0);
        double norm_f, scalar;
        double act_spring_strength, act_rep_force_strength;

        //set cool_factor
        if (coolTemperature() == false)
        {
            cool_factor = 1.0;
        }
        else if ((coolTemperature() == true) && (fine_tuning_step == 0))
        {
            if (iter == 1)
            {
                cool_factor = coolValue();
            }
            else
            {
                cool_factor *= coolValue();
            }
        }

        if (fine_tuning_step == 1)
        {
            cool_factor /= 10.0; //decrease the temperature rapidly
        }
        else if (fine_tuning_step == 2)
        {
            if (iter <= fineTuningIterations() - 5)
            {
                cool_factor = fineTuneScalar(); //decrease the temperature rapidly
            }
            else
            {
                cool_factor = (fineTuneScalar() / 10.0);
            }
        }

        //set the values for the spring strength and strength of the rep. force field
        if (fine_tuning_step <= 1)//usual case
        {
            act_spring_strength = springStrength();
            act_rep_force_strength = repForcesStrength();
        }
        else if (!adjustPostRepStrengthDynamically())
        {
            act_spring_strength = postSpringStrength();
            act_rep_force_strength = postStrengthOfRepForces();
        }
        else //adjustPostRepStrengthDynamically())
        {
            act_spring_strength = postSpringStrength();
            act_rep_force_strength = get_post_rep_force_strength(G.numberOfNodes());
        }

        for (Iterator<node> i = G.nodesIterator(); i.hasNext();)
        {
            v = i.next();
            f.m_x = act_spring_strength * F_attr.get(v).m_x + act_rep_force_strength * F_rep.get(v).m_x;
            f.m_y = act_spring_strength * F_attr.get(v).m_y + act_rep_force_strength * F_rep.get(v).m_y;
            f.m_x = average_ideal_edgelength * average_ideal_edgelength * f.m_x;
            f.m_y = average_ideal_edgelength * average_ideal_edgelength * f.m_y;

            norm_f = f.norm();
            if (f == nullpoint)
            {
                force = nullpoint;
            }
            else if (numexcept.f_near_machine_precision(norm_f, force))
            {
                restrict_force_to_comp_box(force);
            }
            else
            {
                scalar = Math.min(norm_f * cool_factor * forceScalingFactor(),
                        max_radius(iter)) / norm_f;
                force.m_x = scalar * f.m_x;
                force.m_y = scalar * f.m_y;
            }
            F.set(v, force);
        }
    }

    void move_nodes(
            Graph G,
            NodeArray<NodeAttributes> A,
            NodeArray<DPoint> F)
    {
        node v;

        for (Iterator<node> i = G.nodesIterator(); i.hasNext();)
        {
            v = i.next();
            A.get(v).set_position(A.get(v).get_position().plus(F.get(v)));
        }
    }

    void update_boxlength_and_cornercoordinate(
            Graph G,
            NodeArray<NodeAttributes> A)
    {
        node v;
        double xmin, xmax, ymin, ymax;
        DPoint midpoint;

        v = G.firstNode();
        midpoint = A.get(v).get_position();
        xmin = xmax = midpoint.m_x;
        ymin = ymax = midpoint.m_y;

        for (Iterator<node> i = G.nodesIterator(); i.hasNext();)
        {
            v = i.next();
            midpoint = A.get(v).get_position();
            if (midpoint.m_x < xmin)
            {
                xmin = midpoint.m_x;
            }
            if (midpoint.m_x > xmax)
            {
                xmax = midpoint.m_x;
            }
            if (midpoint.m_y < ymin)
            {
                ymin = midpoint.m_y;
            }
            if (midpoint.m_y > ymax)
            {
                ymax = midpoint.m_y;
            }
        }

        //set down_left_corner and boxlength

        down_left_corner.m_x = Math.floor(xmin - 1);
        down_left_corner.m_y = Math.floor(ymin - 1);
        boxlength = Math.ceil(Math.max(ymax - ymin, xmax - xmin) * 1.01 + 2);

        //exception handling: all nodes have same x and y coordinate
        if (boxlength <= 2)
        {
            boxlength = G.numberOfNodes() * 20;
            down_left_corner.m_x = Math.floor(xmin) - (boxlength / 2);
            down_left_corner.m_y = Math.floor(ymin) - (boxlength / 2);
        }

        //export the boxlength and down_left_corner values to the rep. calc. classes

        if (repulsiveForcesCalculation() == RepulsiveForcesMethod.rfcExact ||
                repulsiveForcesCalculation() == RepulsiveForcesMethod.rfcGridApproximation)
        {
            FR.update_boxlength_and_cornercoordinate(boxlength, down_left_corner);
        }
        else //repulsiveForcesCalculation() == rfcNMM
        {
            NM.update_boxlength_and_cornercoordinate(boxlength, down_left_corner);
        }
    }

    //! Describes the max. radius of a move in one time step, depending on the number of iterations.
    double max_radius(int iter)
    {
        return (iter == 1) ? boxlength / 1000 : boxlength / 5;
    }

    void set_average_ideal_edgelength(
            Graph G,
            EdgeArray<EdgeAttributes> E)
    {
        double averagelength = 0;
        edge e;

        if (G.numberOfEdges() > 0)
        {
            for (Iterator<edge> i = G.edgesIterator(); i.hasNext();)
            {
                e = i.next();
                averagelength += E.get(e).get_length();
            }
            average_ideal_edgelength = averagelength / G.numberOfEdges();
        }
        else
        {
            average_ideal_edgelength = 50;
        }
    }

    double get_average_forcevector_length(Graph G, NodeArray<DPoint> F)
    {
        double lengthsum = 0;
        node v;
        for (Iterator<node> i = G.nodesIterator(); i.hasNext();)
        {
            v = i.next();
            lengthsum += F.get(v).norm();
        }
        lengthsum /= G.numberOfNodes();
        return lengthsum;
    }

    void prevent_oscilations(
            Graph G,
            NodeArray<DPoint> F,
            NodeArray<DPoint> last_node_movement,
            int iter)
    {

        double pi_times_1_over_6 = 0.52359878;
        double pi_times_2_over_6 = 2 * pi_times_1_over_6;
        double pi_times_3_over_6 = 3 * pi_times_1_over_6;
        double pi_times_4_over_6 = 4 * pi_times_1_over_6;
        double pi_times_5_over_6 = 5 * pi_times_1_over_6;
        double pi_times_7_over_6 = 7 * pi_times_1_over_6;
        double pi_times_8_over_6 = 8 * pi_times_1_over_6;
        double pi_times_9_over_6 = 9 * pi_times_1_over_6;
        double pi_times_10_over_6 = 10 * pi_times_1_over_6;
        double pi_times_11_over_6 = 11 * pi_times_1_over_6;

        DPoint nullpoint = new DPoint(0, 0);
        double fi; //angle in [0,2pi) measured counterclockwise
        double norm_old, norm_new, quot_old_new;

        if (iter > 1) //usual case
        {//if1
            node v;
            for (Iterator<node> i = G.nodesIterator(); i.hasNext();)
            {
                v = i.next();
                DPoint force_new = new DPoint(F.get(v).m_x, F.get(v).m_y);
                DPoint force_old = new DPoint(last_node_movement.get(v).m_x, last_node_movement.get(v).m_y);
                norm_new = F.get(v).norm();
                norm_old = last_node_movement.get(v).norm();
                if ((norm_new > 0) && (norm_old > 0))
                {//if2
                    quot_old_new = norm_old / norm_new;

                    //prevent oszilations
                    fi = angle(nullpoint, force_old, force_new);
                    if (((fi <= pi_times_1_over_6) || (fi >= pi_times_11_over_6)) &&
                            ((norm_new > (norm_old * 2.0))))
                    {
                        F.get(v).m_x = quot_old_new * 2.0 * F.get(v).m_x;
                        F.get(v).m_y = quot_old_new * 2.0 * F.get(v).m_y;
                    }
                    else if ((fi >= pi_times_1_over_6) && (fi <= pi_times_2_over_6) &&
                            (norm_new > (norm_old * 1.5)))
                    {
                        F.get(v).m_x = quot_old_new * 1.5 * F.get(v).m_x;
                        F.get(v).m_y = quot_old_new * 1.5 * F.get(v).m_y;
                    }
                    else if ((fi >= pi_times_2_over_6) && (fi <= pi_times_3_over_6) &&
                            (norm_new > (norm_old)))
                    {
                        F.get(v).m_x = quot_old_new * F.get(v).m_x;
                        F.get(v).m_y = quot_old_new * F.get(v).m_y;
                    }
                    else if ((fi >= pi_times_3_over_6) && (fi <= pi_times_4_over_6) &&
                            (norm_new > (norm_old * 0.66666666)))
                    {
                        F.get(v).m_x = quot_old_new * 0.66666666 * F.get(v).m_x;
                        F.get(v).m_y = quot_old_new * 0.66666666 * F.get(v).m_y;
                    }
                    else if ((fi >= pi_times_4_over_6) && (fi <= pi_times_5_over_6) &&
                            (norm_new > (norm_old * 0.5)))
                    {
                        F.get(v).m_x = quot_old_new * 0.5 * F.get(v).m_x;
                        F.get(v).m_y = quot_old_new * 0.5 * F.get(v).m_y;
                    }
                    else if ((fi >= pi_times_5_over_6) && (fi <= pi_times_7_over_6) &&
                            (norm_new > (norm_old * 0.33333333)))
                    {
                        F.get(v).m_x = quot_old_new * 0.33333333 * F.get(v).m_x;
                        F.get(v).m_y = quot_old_new * 0.33333333 * F.get(v).m_y;
                    }
                    else if ((fi >= pi_times_7_over_6) && (fi <= pi_times_8_over_6) &&
                            (norm_new > (norm_old * 0.5)))
                    {
                        F.get(v).m_x = quot_old_new * 0.5 * F.get(v).m_x;
                        F.get(v).m_y = quot_old_new * 0.5 * F.get(v).m_y;
                    }
                    else if ((fi >= pi_times_8_over_6) && (fi <= pi_times_9_over_6) &&
                            (norm_new > (norm_old * 0.66666666)))
                    {
                        F.get(v).m_x = quot_old_new * 0.66666666 * F.get(v).m_x;
                        F.get(v).m_y = quot_old_new * 0.66666666 * F.get(v).m_y;
                    }
                    else if ((fi >= pi_times_9_over_6) && (fi <= pi_times_10_over_6) &&
                            (norm_new > (norm_old)))
                    {
                        F.get(v).m_x = quot_old_new * F.get(v).m_x;
                        F.get(v).m_y = quot_old_new * F.get(v).m_y;
                    }
                    else if ((fi >= pi_times_10_over_6) && (fi <= pi_times_11_over_6) &&
                            (norm_new > (norm_old * 1.5)))
                    {
                        F.get(v).m_x = quot_old_new * 1.5 * F.get(v).m_x;
                        F.get(v).m_y = quot_old_new * 1.5 * F.get(v).m_y;
                    }
                }//if2
                last_node_movement.set(v, F.get(v));
            }
        }//if1
        else if (iter == 1)
        {
            init_last_node_movement(G, F, last_node_movement);
        }
    }

    double angle(DPoint P, DPoint Q, DPoint R)
    {
        double dx1 = Q.m_x - P.m_x;
        double dy1 = Q.m_y - P.m_y;
        double dx2 = R.m_x - P.m_x;
        double dy2 = R.m_y - P.m_y;
        double fi;//the angle

        if ((dx1 == 0 && dy1 == 0) || (dx2 == 0 && dy2 == 0))
        {
            System.out.println("Multilevel::angle()");
        }

        double norm = (dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2);
        double cosfi = (dx1 * dx2 + dy1 * dy2) / Math.sqrt(norm);

        if (cosfi >= 1.0)
        {
            fi = 0;
        }
        if (cosfi <= -1.0)
        {
            fi = Math.PI;
        }
        else
        {
            fi = Math.acos(cosfi);
            if (dx1 * dy2 < dy1 * dx2)
            {
                fi = -fi;
            }
            if (fi < 0)
            {
                fi += 2 * Math.PI;
            }
        }
        return fi;
    }

    void init_last_node_movement(
            Graph G,
            NodeArray<DPoint> F,
            NodeArray<DPoint> last_node_movement)
    {
        node v;
        for (Iterator<node> i = G.nodesIterator(); i.hasNext();)
        {
            v = i.next();
            last_node_movement.set(v, F.get(v));
        }
    }

    void adapt_drawing_to_ideal_average_edgelength(
            Graph G,
            NodeArray<NodeAttributes> A,
            EdgeArray<EdgeAttributes> E)
    {
        edge e;
        node v;
        double sum_real_edgelength = 0;
        double sum_ideal_edgelength = 0;
        double area_scaling_factor;
        DPoint new_pos = new DPoint();

        for (Iterator<edge> i = G.edgesIterator(); i.hasNext();)
        {
            e = i.next();
            sum_ideal_edgelength += E.get(e).get_length();
            sum_real_edgelength += (A.get(e.source()).get_position().minus(A.get(e.target()).get_position())).norm();
        }

        if (sum_real_edgelength == 0) //very very unlike case
        {
            area_scaling_factor = 1;
        }
        else
        {
            area_scaling_factor = sum_ideal_edgelength / sum_real_edgelength;
        }

        for (Iterator<node> i = G.nodesIterator(); i.hasNext();)
        {
            v = i.next();
            new_pos.m_x = resizingScalar() * area_scaling_factor * A.get(v).get_position().m_x;
            new_pos.m_y = resizingScalar() * area_scaling_factor * A.get(v).get_position().m_y;
            A.get(v).set_position(new_pos);
        }
    }

    void restrict_force_to_comp_box(DPoint force)
    {
        double x_min = down_left_corner.m_x;
        double x_max = down_left_corner.m_x + boxlength;
        double y_min = down_left_corner.m_y;
        double y_max = down_left_corner.m_y + boxlength;
        if (force.m_x < x_min)
        {
            force.m_x = x_min;
        }
        else if (force.m_x > x_max)
        {
            force.m_x = x_max;
        }
        if (force.m_y < y_min)
        {
            force.m_y = y_min;
        }
        else if (force.m_y > y_max)
        {
            force.m_y = y_max;
        }
    }
}
