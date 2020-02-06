/*
 * Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 */

package net.biomodels.jummp.plugins.format

import grails.test.mixin.*
import grails.test.mixin.services.ServiceUnitTestMixin
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import spock.lang.Specification

@TestMixin(ServiceUnitTestMixin)
@TestFor(MatlabFormatService)
class MatlabFormatServiceSpec extends Specification {
    File tmpFolder = new File("target/testFiles/")

    static doWithSpring = {
        modelFileFormatService(MockFormatService) { bean ->
            bean.autowire = 'byName'
        }
    }

    def setup() {
        expect:
        tmpFolder.mkdirs()
    }

    def cleanup() {
        expect:
        tmpFolder.listFiles()*.delete()
        tmpFolder.delete()
    }

    void "test getFormatVersion returns the expected values"() {
        expect:
        null == service.getFormatVersion(null)
        null == service.getFormatVersion(new RevisionTransportCommand())
        service.FORMAT_VERSION == service.getFormatVersion(new RevisionTransportCommand(format:
                new ModelFormatTransportCommand(identifier: 'matlab')))
        null == service.getFormatVersion(new RevisionTransportCommand(format:
                new ModelFormatTransportCommand(identifier: 'foo')))
    }

    void "test areFilesThisFormat"() {
        expect: 'areFilesThisFormat returns false for rubbish input'
        !service.areFilesThisFormat(null)
        !service.areFilesThisFormat([])

        when: 'I pass a list of files and none of them is a Matlab script'
        def files = new File('.').listFiles(new FileFilter() {
            boolean accept(File f) {
                f.isFile() && f.canRead()
            }
        }).toList()

        then: 'areFilesThisFormat returns false'
        !service.areFilesThisFormat(files)

        when: 'a matlab function file is provided'
        def f = File.createTempFile("matlab", ".m", tmpFolder)
        f.text = """function b=wtsgaus(p,N)
% wtsgaus: weights for gaussian filter with specified frequency response
% b=wtsgaus(p,N);
% Last revised 2003-3-14
%
% Weights for gaussian filter with specified frequency response
% Specify te wavelength for the 0.50 respons, and the length of series, get
% the coefficients, or weights
%
%*** INPUT
%
% p (1 x 1)i  period (years) at which filter is to have amp frequency response of 0.5
% N (1 x 1)i  length of the time series (number of observations)
%
%*** OUTPUT
%
% b (1 x n)r  computed weights
%
%
%*** REFERENCES
%
% WMO 1966, p. 47
%
%*** UW FUNCTIONS CALLED -- NONE
%*** TOOLBOXES NEEDED -- stats
%
%*** NOTES
%
% Amplitude of frequency response drops to 0.50 at a wavelength of
% about 6 standard deviations of the appropriate guassian curve
%
% N is used as an input to restict the possible filter size (number of weights) to
%  no larger than the sample length

if p>N;
    error(['Desired 50% period ' num2str(p) ' is greater than  the sample length ' int2str(N)]);
end;


% Check that period of 50% response at least 5 yr
if p<5;
   error('Period of 50% response must be at least 5 yr');
end;

sigma=p/6;  % Gaussian curve should have this standard deviation

x=-N:N;
b=normpdf(x/sigma,0,1);
bmax=max(b);
bkeep = b>=0.05*bmax; % keep weights at least 5% as big as central weight
b=b(bkeep);
b=b/sum(b); % force weights to sum to one

%% script taken from https://issues.apache.org/jira/browse/TIKA-1634
"""

        then: 'areFilesThisFormat returns true'
        service.areFilesThisFormat([f])

        when: 'a list of files containing a matlab script is provided'
        def f1 = File.createTempFile("matlab", '.m', tmpFolder)
        f1.text = """%% Sample matlab script
%% https://uk.mathworks.com/help/matlab/examples/creating-the-matlab-logo.html
L = 160*membrane(1,100);
f = figure;
ax = axes;

s = surface(L);
s.EdgeColor = 'none';
view(3)

ax.XLim = [1 201];
ax.YLim = [1 201];
ax.ZLim = [-53.4 160];

ax.CameraPosition = [-145.5 -229.7 283.6];
ax.CameraTarget = [77.4 60.2 63.9];
ax.CameraUpVector = [0 0 1];
ax.CameraViewAngle = 36.7;
ax.Position = [0 0 1 1];
ax.DataAspectRatio = [1 1 .9];

l1 = light;
l1.Position = [160 400 80];
l1.Style = 'local';
l1.Color = [0 0.8 0.8];

l2 = light;
l2.Position = [.5 -1 .4];
l2.Color = [0.8 0.8 0];

s.FaceColor = [0.9 0.2 0.2];
view(3)
"""
        files = [f1]

        then: 'areFilesThisFormat returns true'
        service.areFilesThisFormat files

        when: 'the file is empty'
        def f2 = File.createTempFile("matlab", '.m', tmpFolder)
        f2.text = ""

        then: 'areFilesThisFormat returns false'
        !service.areFilesThisFormat([f2])
    }
}

/**
 * Mock ModelFileFormatService implementation to be used for this unit tests.
 *
 * The class under test needs to be able to call the ModelFileFormatService bean
 * in order to validate requests
 * Its reference implementation is in the main application.
 */
class MockFormatService {
    /**
     * Dependency injected
     */
    def applicationContext

    def serviceForFormat(def formatCmd) {
        formatCmd.identifier == 'matlab' ? applicationContext.getBean('matlabFormatService') : null
    }
}
