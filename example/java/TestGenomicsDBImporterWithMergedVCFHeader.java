/**
 * The MIT License (MIT)
 * Copyright (c) 2016 Intel Corporation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of 
 * this software and associated documentation files (the "Software"), to deal in 
 * the Software without restriction, including without limitation the rights to 
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all 
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import com.intel.genomicsdb.ChromosomeInterval;
import com.intel.genomicsdb.GenomicsDBException;
import com.intel.genomicsdb.GenomicsDBImporter;
import htsjdk.tribble.AbstractFeatureReader;
import htsjdk.tribble.FeatureReader;
import htsjdk.tribble.readers.LineIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLine;
import htsjdk.variant.vcf.VCFUtils;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.*;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;


/**
 * Created by kdatta1 on 3/10/17.
 */
public final class TestGenomicsDBImporterWithMergedVCFHeader {

  public enum ArgsIdxEnum
  {
    ARGS_IDX_USE_SAMPLES_IN_ORDER(1000),
    ARGS_IDX_FAIL_IF_UPDATING(1001),
    ARGS_IDX_AFTER_LAST_ARG_IDX(1002);

    private final int mArgsIdx;
    ArgsIdxEnum(final int idx)
    {
      mArgsIdx = idx;
    }

    int idx()
    {
      return mArgsIdx;
    }
  }
  public static void main(final String[] args)
    throws IOException, GenomicsDBException, ParseException
  {
    final int firstEnumIdx = ArgsIdxEnum.ARGS_IDX_USE_SAMPLES_IN_ORDER.idx();
    LongOpt[] longopts = new LongOpt[5];
    longopts[0] = new LongOpt("use_samples_in_order", LongOpt.NO_ARGUMENT, null, ArgsIdxEnum.ARGS_IDX_USE_SAMPLES_IN_ORDER.idx());
    longopts[1] = new LongOpt("fail_if_updating", LongOpt.NO_ARGUMENT, null, ArgsIdxEnum.ARGS_IDX_FAIL_IF_UPDATING.idx());
    longopts[2] = new LongOpt("interval", LongOpt.REQUIRED_ARGUMENT, null, 'L');
    longopts[3] = new LongOpt("workspace", LongOpt.REQUIRED_ARGUMENT, null, 'w');
    longopts[4] = new LongOpt("array", LongOpt.REQUIRED_ARGUMENT, null, 'A');
    //Arg parsing
    Getopt g = new Getopt("TestGenomicsDBImporterWithMergedVCFHeader", args, "w:A:L:", longopts);
    int c = -1;
    String optarg;
    //Array of enums
    final ArgsIdxEnum[] enumArray = ArgsIdxEnum.values();
    boolean useSamplesInOrder = false;
    boolean failIfUpdating = false;
    String workspace = "";
    String arrayName = "";
    String chromosomeInterval = "";
    while ((c = g.getopt()) != -1)
    {
      switch(c)
      {
        case 'w':
          workspace = g.getOptarg();
          break;
        case 'A':
          arrayName = g.getOptarg();
          break;
        case 'L':
          chromosomeInterval = g.getOptarg();
          break;
        default:
          {
            if(c >= firstEnumIdx && c < ArgsIdxEnum.ARGS_IDX_AFTER_LAST_ARG_IDX.idx())
            {
              int offset = c - firstEnumIdx;
              assert offset < enumArray.length;
              switch(enumArray[offset])
              {
                case ARGS_IDX_USE_SAMPLES_IN_ORDER:
                  useSamplesInOrder = true;
                  break;
                case ARGS_IDX_FAIL_IF_UPDATING:
                  failIfUpdating = true;
                  break;
                default:
                  System.err.println("Unknown command line option "+g.getOptarg()+" - ignored");
                  break;
              }
            }
            else
              System.err.println("Unknown command line option "+g.getOptarg()+" - ignored");
          }
      }
    }
    int numPositionalArgs = args.length - g.getOptind();
    if (numPositionalArgs <= 0
        || arrayName.isEmpty() || workspace.isEmpty()
        || chromosomeInterval.isEmpty()
        ) {
      System.out.println("Usage: ExampleGenomicsDBImporter" + " -L chromosome:interval " +
          "-w genomicsdbworkspace -A arrayname variantfile(s) [--use_samples_in_order --fail_if_updating]");
      System.exit(-1);
    }

    String[] temp0 = chromosomeInterval.split(":");
    String chromosomeName = temp0[0];
    String[] interval = temp0[1].split("-");
    List<String> files = new ArrayList<>();
    for (int i = g.getOptind(); i < args.length; ++i) {
      files.add(args[i]);
    }

    Map<String, FeatureReader<VariantContext>> map = new LinkedHashMap<>();
    List<VCFHeader> headers = new ArrayList<>();

    for (String file : files) {
      AbstractFeatureReader<VariantContext, LineIterator> reader =
        AbstractFeatureReader.getFeatureReader(file, new VCFCodec(), false);
      String sampleName = ((VCFHeader) reader.getHeader()).getGenotypeSamples().get(0);
      headers.add((VCFHeader) reader.getHeader());
      map.put(sampleName, reader);
    }

    Set<VCFHeaderLine> mergedHeader = VCFUtils.smartMergeHeaders(headers, true);

    GenomicsDBImporter importer = new GenomicsDBImporter(
        map, mergedHeader,
        new ChromosomeInterval(chromosomeName, Integer.parseInt(interval[0]), Integer.parseInt(interval[1])),
        workspace, arrayName, 1000L, 1048576L,
        useSamplesInOrder, failIfUpdating);
    boolean isdone = importer.importBatch();
    assert (isdone);
  }
}