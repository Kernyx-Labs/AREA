import 'package:flutter/material.dart';

/// Shared layout constants so the canvas, nodes and connectors stay aligned.
class PipelineLayout {
	const PipelineLayout._();

	static const double nodeWidth = 220;
	static const double nodeHeight = 172;
	static const double nodeSpacing = 36;

	static const double connectorSize = 24;
	static const double connectorRadius = connectorSize / 2;
	static const double connectorOverlap = 12; // how far the handle extends past the node

	static const double connectionCurveInset = 64;

	static const Offset initialNodePosition = Offset(40, 160);
}

