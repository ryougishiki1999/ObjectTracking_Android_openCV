import numpy as np
import math
import time
import cv2
import datetime


intrinsic = np.array([610.6543579101562, 0, 399.5, 0,
                     610.6543579101562, 299.5,  0, 0, 1], dtype=np.float64)
intrinsic = intrinsic.reshape(3, 3)
print(intrinsic)

discoeffs = np.array([0.2673602700233459, -1.091482043266296,
                     0, 0, 1.180935621261597], dtype=np.float64)
discoeffs = discoeffs.reshape(5, 1)
print(discoeffs)


def isRotationMatrix(R):
    Rt = np.transpose(R)
    shouldBeIdentity = np.dot(Rt, R)
    I = np.identity(3, dtype=R.dtype)
    n = np.linalg.norm(I - shouldBeIdentity)
    return n < 1e-6


def rotationMatrixToEulerAngles(R):

    assert(isRotationMatrix(R))

    sy = math.sqrt(R[2, 1] * R[2, 1] + R[2, 2] * R[2, 2])

    singular = sy < 1e-6

    if not singular:
        x = math.atan2(R[2, 1], R[2, 2])
        y = math.atan2(-R[2, 0], sy)
        z = math.atan2(R[1, 0], R[0, 0])
    else:
        x = math.atan2(-R[1, 2], R[1, 1])
        y = math.atan2(-R[2, 0], sy)
        z = 0

    x = convertoDegree(x)
    y = convertoDegree(y)
    z = convertoDegree(z)

    return np.array([x, y, z], dtype=np.float64)


def transformConvertToRT(transform: np.ndarray, R, T):
    np.copyto(R, transform[0:3, 0:3])
    np.copyto(T, transform[0:3, 3:4])
    # print(R)
    # print(T)


def RTConverToTransform(R, T, transform: np.ndarray):
    RT = np.hstack([R, T])
    np.copyto(transform[0:3, 0:4], RT)


def convertoDegree(theta):
    degree = math.degrees(theta)
    return degree


def convertoTheta(degree):
    theta = math.radians(float(degree))
    return theta


def computeRX(theta):
    RX = np.zeros((3, 3), dtype=np.float64)
    RX[0, 0] = 1
    RX[1, 1] = RX[2, 2] = math.cos(theta)
    RX[1, 2] = -math.sin(theta)
    RX[2, 1] = math.sin(theta)

    print(RX)
    return RX


def computeRY(theta):
    RY = np.zeros((3, 3), dtype=np.float64)
    RY[1, 1] = 1
    RY[0, 0] = RY[2, 2] = math.cos(theta)
    RY[2, 0] = -math.sin(theta)
    RY[0, 2] = math.sin(theta)

    # print(RY)
    return RY


def computeRZ(theta):
    RZ = np.zeros((3, 3), dtype=np.float64)
    RZ[2, 2] = 1
    RZ[0, 0] = RZ[1, 1] = math.cos(theta)
    RZ[0, 1] = -math.sin(theta)
    RZ[1, 0] = math.sin(theta)

    # print(RZ)
    return RZ


def run():

    point = (-0.4676005126467835, -0.09352010252935661, 2.5381536960780573)

    pointList = []
    pointList.append(point)
    objectPoints = np.array(pointList, dtype=np.float64)

    R = np.eye(3, dtype=np.float64)
    T = np.ones((3, 1), dtype=np.float64)

    RDelta = np.eye(3, dtype=np.float64)
    TDelta = np.zeros((3, 1), dtype=np.float64)

    transform = np.eye(4, dtype=np.float64)
    print(f"transform:\n{transform}")
    transformDelta = np.eye(4, dtype=np.float64)

    filestr = "transform_"+str(time.time())+"_"+ str(datetime.date.today())+".txt"

    with open(filestr, "w") as file:
        file.write(str(transform)+"\n")
        while True:
            print("=======================\n")
            degreeX = input("输入x角度:")
            degreeY = input("输入y角度:")
            degreeZ = input("输入z角度:")

            thetaX = convertoTheta(degreeX)
            thetaY = convertoTheta(degreeY)
            thetaZ = convertoTheta(degreeZ)

            RX = computeRX(thetaX)
            RY = computeRY(thetaY)
            RZ = computeRZ(thetaZ)

            RDelta = np.dot(np.dot(RZ, RY), RX)
            TDelta = np.zeros((3, 1), dtype=np.float64)

            # print(RDelta)

            RTConverToTransform(RDelta, TDelta, transformDelta)

            print(f"transformDelta:\n{transformDelta}")
            transform = np.dot(transformDelta, transform)
            print(f"transform:\n{transform}")

            transformConvertToRT(transform, R, T)
            rVec = np.ndarray((3, 1), dtype=np.float64)
            cv2.Rodrigues(R, rVec)
            print(rVec)

            pixels, _ = cv2.projectPoints(
                objectPoints, rVec, T, intrinsic, discoeffs)

            print("Euler Angles: ")
            euler = rotationMatrixToEulerAngles(R)
            print(f"x:{euler[0]}, y:{euler[1]}, z:{euler[2]}")

            file.write("\n")
            file.write("X: "+degreeX+"\n")
            file.write("Y: "+degreeY+"\n")
            file.write("Z: "+degreeZ+"\n")
            file.write("transformDelta: \n"+str(transformDelta)+"\n")
            file.write("transform: \n"+str(transform)+"\n")
            file.write("total euler: \n"+str(euler)+"\n")
            file.write("pixel: \n"+str(pixels[0]))
            file.write("\n")


if __name__ == "__main__":
    # print(RDelta)
    # print(TDelta)

    # RTConverToTransform(RDelta, TDelta, transform)

    # transform = np.arange(16)
    # transform = transform.reshape((4, 4))

    # print(transform)
    # transformConvertToRT(transform, RDelta, TDelta)
    run()
