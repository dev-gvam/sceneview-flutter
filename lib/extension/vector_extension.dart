import 'package:vector_math/vector_math_64.dart';

extension Vector3Extension on Vector3 {
  Map<String, double> toMap() {
    return <String, double>{
      'x': x,
      'y': y,
      'z': z,
    };
  }
}
