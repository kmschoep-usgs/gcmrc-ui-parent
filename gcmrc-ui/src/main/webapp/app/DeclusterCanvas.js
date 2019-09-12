/* Copyright (c) 2006-2012 by OpenLayers Contributors (see authors.txt for 
 * full list of contributors). Published under the 2-clause BSD license.
 * See license.txt in the OpenLayers distribution or repository for the
 * full text of the license. */

/**
 * @requires OpenLayers/Renderer.js
 */

/**
 * Class: OpenLayers.Renderer.DeclusterCanvas 
 * A renderer based on the 2D 'canvas' drawing element.
 * 
 * Inherits:
 *  - <OpenLayers.Renderer>
 */
OpenLayers.Renderer.DeclusterCanvas = OpenLayers.Class(OpenLayers.Renderer.Canvas, {
    
    decluster: true,
    declusterCoef: 2,
    declusterStrokeColor: '#ffffff',
    declusterOnCentroid: true,
    declusterThetaAverage: false,
    declusterMinimumRadius: 24,
    declusterDeactiveBuffer: 2,
    
    declusterPixel: null, // state
    declusterRadius: null, // state
    
    /**
     * Constructor: OpenLayers.Renderer.Canvas
     *
     * Parameters:
     * containerID - {<String>}
     * options - {Object} Optional properties to be set on the renderer.
     */
    initialize: function(containerID, options) {
        OpenLayers.Renderer.Canvas.prototype.initialize.apply(this, arguments);
        
        if (!options) {
            options = {};
        }
        this.declusterCoef = options.declusterCoef || 2;
        this.declusterStrokeColor = options.declusterStrokeColor || '#ffffff';
        this.declusterOnCentroid = options.declusterOnCentroid || true;
        this.declusterThetaAverage = options.declusterThetaAverage || false;
        this.declusterMinimumRadius = options.declusterMinimumRadius || 24;
        this.declusterDeactiveBuffer = options.declusterDeactiveBuffer || 2;
        
        if (this.decluster) {
            this.clusterCanvas = document.createElement("canvas");
            this.clusterContext = this.clusterCanvas.getContext("2d");
        }
    },
   
    /**
     * APIMethod: supported
     * 
     * Returns:
     * {Boolean} Whether or not the browser supports the renderer class
     */
    supported: function() {
        return OpenLayers.CANVAS_SUPPORTED;
    }, 
    
    /**
     * Method: setSize
     * Sets the size of the drawing surface.
     *
     * Once the size is updated, redraw the canvas.
     *
     * Parameters:
     * size - {<OpenLayers.Size>} 
     */
    setSize: function(size) {
        OpenLayers.Renderer.Canvas.prototype.setSize.apply(this, arguments);
        if (this.decluster) {
            this.clusterCanvas.style.width = size.w + "px";
            this.clusterCanvas.style.height = size.h + "px";
            this.clusterCanvas.width = size.w;
            this.clusterCanvas.height = size.h;
        }
    },

    /**
     * Method: drawPoint
     * This method is only called by the renderer itself.
     * 
     * Parameters: 
     * geometry - {<OpenLayers.Geometry>}
     * style    - {Object}
     * featureId - {String}
     */ 
    drawPoint: function(geometry, style, featureId) {
        if(style.graphic !== false) {
            if(style.externalGraphic) {
                this.drawExternalGraphic(geometry, style, featureId);
            } else if (style.graphicName && (style.graphicName != "circle")) {
                this.drawNamedSymbol(geometry, style, featureId);
            } else {
                var pt = this.getLocalXY(geometry);
                var p0 = pt[0];
                var p1 = pt[1];
                if(!isNaN(p0) && !isNaN(p1)) {
                    var twoPi = Math.PI*2;
                    var radius = style.pointRadius;
                    if(style.fill !== false) {
                        this.setCanvasStyle("fill", style);
                        this.canvas.beginPath();
                        this.canvas.arc(p0, p1, radius, 0, twoPi, true);
                        this.canvas.fill();
                        if (this.hitDetection) {
                            this.setHitContextStyle("fill", featureId, style);
                            this.hitContext.beginPath();
                            this.hitContext.arc(p0, p1, radius, 0, twoPi, true);
                            this.hitContext.fill();
                        }
                    }

                    if(style.stroke !== false) {
                        this.setCanvasStyle("stroke", style);
                        this.canvas.beginPath();
                        this.canvas.arc(p0, p1, radius, 0, twoPi, true);
                        this.canvas.stroke();
                        if (this.hitDetection) {
                            this.setHitContextStyle("stroke", featureId, style);
                            this.hitContext.beginPath();
                            this.hitContext.arc(p0, p1, radius, 0, twoPi, true);
                            this.hitContext.stroke();
                        }
                        this.setCanvasStyle("reset");
                    }
                    
                    if (this.hitDetection) {
                        this.clusterContext.fillStyle = "rgb(1,0,0)";
                        this.clusterContext.globalCompositeOperation = "lighter";
                        this.clusterContext.beginPath();
                        this.clusterContext.arc(p0, p1, radius * this.declusterCoef, 0, twoPi);
                        this.clusterContext.fill();
                        this.clusterContext.globalCompositeOperation = "source-over";
                    }
                }
            }
        }
    },
            
    drawClusterPoint: function(geometry, style, featureId) {
        if(style.graphic !== false) {
            if(style.externalGraphic) {
                this.drawExternalGraphic(geometry, style, featureId);
            } else if (style.graphicName && (style.graphicName != "circle")) {
                this.drawNamedSymbol(geometry, style, featureId);
            } else {
                var pt = this.getLocalXY(geometry);
                var p0 = pt[0];
                var p1 = pt[1];
                if(!isNaN(p0) && !isNaN(p1)) {
                    var twoPi = Math.PI*2;
                    var radius = style.pointRadius;
                    if(style.fill !== false) {
                        this.setCanvasStyle("fill", style);
                        this.canvas.beginPath();
                        this.canvas.arc(p0, p1, radius, 0, twoPi, true);
                        this.canvas.fill();
                        if (this.hitDetection) {
                            this.setHitContextStyle("fill", featureId, style);
                            this.hitContext.beginPath();
                            this.hitContext.arc(p0, p1, radius, 0, twoPi, true);
                            this.hitContext.fill();
                        }
                    }

                    if(style.stroke !== false) {
                        this.setCanvasStyle("stroke", style);
                        this.canvas.beginPath();
                        this.canvas.arc(p0, p1, radius, 0, twoPi, true);
                        this.canvas.stroke();
                        if (this.hitDetection) {
                            this.setHitContextStyle("stroke", featureId, style);
                            this.hitContext.beginPath();
                            this.hitContext.arc(p0, p1, radius, 0, twoPi, true);
                            this.hitContext.stroke();
                        }
                        this.setCanvasStyle("reset");
                    }
                    
                    // START - HACK
                    pt = this.getLocalXY(geometry.orig);
                    var o0 = pt[0]
                    var o1 = pt[1];
                    this.setCanvasStyle("stroke", style);
                    this.canvas.beginPath();
                    this.canvas.strokeStyle = this.declusterStrokeColor;
                    var dx = o0 - p0;
                    var dy = o1 - p1;
                    var theta = Math.atan2(dy,dx);
                    this.canvas.moveTo(p0 + Math.cos(theta) * radius ,p1 + Math.sin(theta) * radius);
                    this.canvas.lineTo(o0, o1);
                    this.canvas.stroke();
                    this.setCanvasStyle("reset");
                    // END - HACK
                    
                    if (this.hitDetection) {
                        this.clusterContext.fillStyle = "rgb(1,0,0)";
                        this.clusterContext.globalCompositeOperation = "lighter";
                        this.clusterContext.beginPath();
                        this.clusterContext.arc(p0, p1, radius * this.declusterCoef, 0, twoPi);
                        this.clusterContext.fill();
                        this.clusterContext.globalCompositeOperation = "source-over";
                    }
                    
                }
            }
        }
    },

    /**
     * Method: clear
     * Clear all vectors from the renderer.
     */    
    clear: function() {
        OpenLayers.Renderer.Canvas.prototype.clear.apply(this, arguments);
        var height = this.root.height;
        var width = this.root.width;
        if (this.decluster) {
            this.clusterContext.clearRect(0, 0, width, height);
        }
    },

    /**
     * Method: getFeatureIdFromEvent
     * Returns a feature id from an event on the renderer.  
     * 
     * Parameters:
     * evt - {<OpenLayers.Event>} 
     *
     * Returns:
     * {<OpenLayers.Feature.Vector} A feature or undefined.  This method returns a 
     *     feature instead of a feature id to avoid an unnecessary lookup on the
     *     layer.
     */
    getFeatureIdFromEvent: function(evt) {
        var featureId, feature;
        
        if (this.hitDetection && this.root.style.display !== "none") {
            // this dragging check should go in the feature handler
            if (!this.map.dragging) {
                var xy = evt.xy;
                var x = xy.x | 0;
                var y = xy.y | 0;
                
                var inCluster = false;
                if (this.clusterFeatures) {
                    var dx = x - this.declusterPixel.x;
                    var dy = y - this.declusterPixel.y;
                    if (((dx * dx) + (dy * dy)) < (this.declusterRadius * this.declusterRadius)) {
                        inCluster = true;
                    } else {
                        delete this.clusterFeatures;
                        this.redraw();
                    }
                }
                
                if (!inCluster) {
                    var clusterData = this.clusterContext.getImageData(x, y, 1, 1).data;
                    if (clusterData[0] > 1) {
                        var clusterCount = 0;
                        var clusterCentroidX = 0;
                        var clusterCentroidY = 0;
                        var clusterMaxPointRadius = 0;
                        var clusterFeaturesAsArray = new Array();
                        this.indexer.iterate(function(feature, style) {
                            var candidateFeature = feature;
                            var candidateStyle = style;
                            var candidateLocal = this.getLocalXY(candidateFeature.geometry);
                            var candidateRadius = (candidateStyle.pointRadius) * this.declusterCoef;
                            // calculate distance from center of pixel
                            var candidateDistanceX = candidateLocal[0] - (x + 0.5);
                            var candidateDistanceY = candidateLocal[1] - (y + 0.5);
                            if (((candidateDistanceX * candidateDistanceX) + (candidateDistanceY * candidateDistanceY)) < (candidateRadius * candidateRadius)) {
                                var clusterFeature = {__proto__: candidateFeature};
                                var clusterStyle = {__proto__: candidateStyle};
                                var clusterFeatureAsArray = [clusterFeature, clusterStyle];
                                clusterFeaturesAsArray.push(clusterFeatureAsArray);
                                // TODO optimize later, this is subject to fixed precision floating point errors
                                clusterCentroidX += candidateLocal[0];
                                clusterCentroidY += candidateLocal[1];
                                if (clusterMaxPointRadius < candidateStyle.pointRadius) {
                                    clusterMaxPointRadius = candidateStyle.pointRadius;
                                }
                                clusterCount++;
                            }
                        }, this);

                        // calculate theta about centroid
                        clusterCentroidX /= clusterCount;
                        clusterCentroidY /= clusterCount;
                        var clusterCenterX;
                        var clusterCenterY;
                        if (this.declusterOnCentroid) {
                            clusterCenterX = clusterCentroidX;
                            clusterCenterY = clusterCentroidY;
                        } else {
                            clusterCenterX = x + 0.5;
                            clusterCenterY = y + 0.5;
                        }
                        for (var clusterIndex = 0; clusterIndex < clusterCount; ++clusterIndex) {
                            var clusterFeatureAsArray = clusterFeaturesAsArray[clusterIndex];
                            var clusterLocal = this.getLocalXY(clusterFeatureAsArray[0].geometry);
                            var clusterDistanceX = clusterLocal[0] - clusterCenterX;
                            var clusterDistanceY = clusterLocal[1] - clusterCenterY;
                            var clusterTheta = Math.atan2(clusterDistanceY, clusterDistanceX);
                            clusterFeatureAsArray.push(clusterTheta);
                        }

                        // sort by theta.
                        clusterFeaturesAsArray.sort(function(left, right) {
                            return left[2] - right[2];
                        });
                        // TODO:  expand radius if needed based on cluster count
                        var declusterRadius = this.declusterMinimumRadius;
                        var declusterRadiansStart = clusterFeaturesAsArray[0][2];
                        var declusterRadiansStep = 2*Math.PI / clusterCount;
                        var declusterTheta;
                        this.clusterFeatures = {};
                        for (var clusterIndex = 0; clusterIndex < clusterCount; ++clusterIndex) {
                            var clusterFeatureAsArray = clusterFeaturesAsArray[clusterIndex];
                            var clusterTheta = clusterFeatureAsArray[2];
                            if (this.declusterThetaAverage) {
                                declusterTheta = declusterRadiansStart + clusterIndex * declusterRadiansStep;
                            } else {
                                declusterTheta = clusterTheta;
                            }
                            var declusterX = Math.cos(declusterTheta) * declusterRadius;
                            var declusterY = Math.sin(declusterTheta) * declusterRadius;
                            var declusterPixel = new OpenLayers.Pixel(clusterCenterX + declusterX, clusterCenterY + declusterY);
                            var declusterLonLat = this.map.getLonLatFromPixel(declusterPixel);
                            var clusterFeature = clusterFeatureAsArray[0];
                            clusterFeature.geometry = {
                                __proto__:  clusterFeature.geometry,
                                orig:       clusterFeature.geometry}; // HACK
                            clusterFeature.geometry.x = declusterLonLat.lon;
                            clusterFeature.geometry.y = declusterLonLat.lat;
                            this.clusterFeatures[clusterFeature.id] = clusterFeatureAsArray;
                        }
                        this.declusterPixel = new OpenLayers.Pixel(clusterCenterX,clusterCenterY);
                        this.declusterRadius = this.declusterMinimumRadius + clusterMaxPointRadius + this.declusterDeactiveBuffer;
                        this.redraw();
                    }   
                }
                if (inCluster || !this.clusterData) {
                    var data = this.hitContext.getImageData(x, y, 1, 1).data;
                    if (data[3] === 255) { // antialiased
                        var id = data[2] + (256 * (data[1] + (256 * data[0])));
                        if (id) {
                            featureId = "OpenLayers_Feature_Vector_" + (id - 1 + this.hitOverflow);
                            try {
                                feature = this.indexer.features[featureId][0];
                            } catch(err) {
                                // Because of antialiasing on the canvas, when the hit location is at a point where the edge of
                                // one symbol intersects the interior of another symbol, a wrong hit color (and therefore id) results.
                                // todo: set Antialiasing = 'off' on the hitContext as soon as browsers allow it.
                            }
                        }
//                        if (this.clusterFeatures) {
//                            delete this.clusterFeatures;
//                            this.redraw();
//                        }
                    }
                }
            }
        }
        return feature;
    },

    /**
     * Method: redraw
     * The real 'meat' of the function: any time things have changed,
     *     redraw() can be called to loop over all the data and (you guessed
     *     it) redraw it.  Unlike Elements-based Renderers, we can't interact
     *     with things once they're drawn, to remove them, for example, so
     *     instead we have to just clear everything and draw from scratch.
     */
    redraw: function() {
        if (!this.locked) {
            var height = this.root.height;
            var width = this.root.width;
            this.canvas.clearRect(0, 0, width, height);
            if (this.hitDetection) {
                this.hitContext.clearRect(0, 0, width, height);
                this.clusterContext.clearRect(0, 0, width, height);
            }
            var labelMap = [];
            var feature, geometry, style;
            var worldBounds = (this.map.baseLayer && this.map.baseLayer.wrapDateLine) && this.map.getMaxExtent();

            if (this.clusterFeatures) {
                this.indexer.iterate(function(feature, style) {
                    if (!this.clusterFeatures[feature.id]) {
						geometry = feature.geometry;
						this.calculateFeatureDx(geometry.getBounds(), worldBounds);
						this.drawGeometry(geometry, style, feature.id);
						if(style.label) {
							labelMap.push([feature, style]);
						}
					}
                }, this); 
                for (var id in this.clusterFeatures) {
                    if (!this.clusterFeatures.hasOwnProperty(id)) { continue; }
                    feature = this.clusterFeatures[id][0];
                    geometry = feature.geometry;
                    this.calculateFeatureDx(geometry.getBounds(), worldBounds);
                    style = this.indexer.features[id][1];
                    this.drawClusterPoint(geometry, style, feature.id);
                    if(style.label) {
                        labelMap.push([feature, style]);
                    } 
                }
            } else {
                this.indexer.iterate(function(feature, style) {
                    geometry = feature.geometry;
                    this.calculateFeatureDx(geometry.getBounds(), worldBounds);
                    this.drawGeometry(geometry, style, feature.id);
                    if(style.label) {
                        labelMap.push([feature, style]);
                    }
                }, this);
            }
            var item;
            for (var i=0, len=labelMap.length; i<len; ++i) {
                item = labelMap[i];
                this.drawText(item[0].geometry.getCentroid(), item[1]);
            }
        }    
    },

    CLASS_NAME: "OpenLayers.Renderer.DeclusterCanvas"
});



