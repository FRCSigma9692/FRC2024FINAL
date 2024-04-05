package frc.robot.Subsystems;

import com.revrobotics.AbsoluteEncoder;
import com.revrobotics.CANSparkMax;
import com.revrobotics.SparkPIDController;
import com.revrobotics.CANSparkBase.ControlType;
import com.revrobotics.CANSparkBase.IdleMode;
import com.revrobotics.CANSparkLowLevel.MotorType;
import com.revrobotics.SparkAbsoluteEncoder.Type;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Arm extends SubsystemBase {

    private double camHeight = 16.40; // Limelight height from floor
    private double camAngle = 33.0; // Limelight Camera mount angle
    private double speakerTagHeight = 57.05; // AprilTag ID6 Height
    private double ampTagHeight = 53.7; // AprilTag ID1 Height
    private double setDisRobotToTag = 80; // Inch
    public double pos;

    // private double sourceID = 1;
    // private double speakerID = 2;
    // private double ampID = 3;
    // private double stageID = 4;

    private double actualDistance;

    NetworkTable table = NetworkTableInstance.getDefault().getTable("limelight");

    NetworkTableEntry tx = table.getEntry("tx");
    NetworkTableEntry ty = table.getEntry("ty");
    NetworkTableEntry ry = table.getEntry("ry");
    NetworkTableEntry ta = table.getEntry("ta");
    NetworkTableEntry tv = table.getEntry("tv");
    NetworkTableEntry tID = table.getEntry("tid");

    private double targetX;
    private double targetY;
    private double targetRY;
    private double targetA;
    private int targetV;
    private int targetID;

    final double kP = 0.02;

    double error = 0;
    double output = 0;

    double setAngle = 25;
    double lastDistance;
    double angle;
    // End Limelight -----------------

    double i = 1;
    public CANSparkMax l_Up;
    public SparkPIDController l_Up_pid;
    // public RelativeEncoder l_Up_enc;
    public AbsoluteEncoder l_UpAbsoluteEncoder;

    public CANSparkMax r_Up;
    public SparkPIDController r_Up_pid;
    // public RelativeEncoder r_Up_enc;
    public AbsoluteEncoder r_UpAbsoluteEncoder;

    public double Acceleration;
    public double Deceleration;
    public double currentpos;
    public double desiredstate; 
    public double minpov =0;
    public double maxpov=10;

    // initial height starts from 65
    public Arm() {

        // for absolute encodder
        l_Up = new CANSparkMax(9, MotorType.kBrushless);
        l_UpAbsoluteEncoder = l_Up.getAbsoluteEncoder(Type.kDutyCycle);
        l_Up_pid = l_Up.getPIDController();
        l_Up_pid.setFeedbackDevice(l_UpAbsoluteEncoder);
        l_Up.setIdleMode(IdleMode.kBrake);
        l_Up_pid.setP(1.3);
        l_Up_pid.setD(0);
        l_Up_pid.setFF(0.002);
        l_Up_pid.setPositionPIDWrappingEnabled(true);
        l_Up.burnFlash();

        r_Up = new CANSparkMax(10, MotorType.kBrushless);
        r_UpAbsoluteEncoder = r_Up.getAbsoluteEncoder(Type.kDutyCycle);
        // r_Up_pid = r_Up.getPIDController();
        // r_Up_pid.setFeedbackDevice(r_UpAbsoluteEncoder);
        r_Up.setIdleMode(IdleMode.kBrake);
        // r_Up_pid.setP(0.1);
        // r_Up_pid.setFF(0);
        // r_Up_pid.setPositionPIDWrappingEnabled(true);
        r_Up.follow(l_Up, true);
        r_Up.burnFlash();
        pos = Math.toDegrees(l_UpAbsoluteEncoder.getPosition());


    }

    public void armUp(double B1) {
        double pos = (((Math.toDegrees(l_UpAbsoluteEncoder.getPosition())
                + Math.toDegrees(r_UpAbsoluteEncoder.getPosition()))) / 2.0);
        if (pos < 180) {
            l_Up.set((B1)); // -(b1-0)
        } // (b1-0)
        else {
            l_Up.set(0);
            r_Up.set(0);
        }

    }

    public void armDown(double B1) {
        pos = Math.toDegrees(l_UpAbsoluteEncoder.getPosition());
        if(pos>95){
            l_Up.set(-(B1));
        }
        else if (pos<95 && pos > 69) {
            // double pow = B1/(pos-76);

            l_Up.set(-(B1*(pos-68)*0.025));
        }
        else {
            l_Up.set(0);
        }
    }

    public void armTo(double degree) {// Amp is 4.211 for left n 4.248 for right 243 deg
        // double pos = (((Math.toDegrees(l_UpAbsoluteEncoder.getPosition())
        //         + Math.toDegrees(r_UpAbsoluteEncoder.getPosition()))) / 2.0);

        // desiredState = new TrapezoidProfile.State(sp,0);
        // var setpoint = profile.calculate(0.002, currentState, desiredState);
      
        // l_Up_pid.setReference(Math.toRadians(setpoint.position), ControlType.kPosition); //ALWAYS USE RADIANS FOR SETREF!!
        l_Up_pid.setReference(Math.toRadians(degree), ControlType.kPosition); //ALWAYS USE RADIANS FOR SETREF!!
        // r_Up_pid.setReference(sp, ControlType.kPosition);
        // if (pos > sp + 10) {
        //     l_Up.set(-0.6);
        //     r_Up.set(-0.6);
        // } else if (pos < sp - 10) {
        //     l_Up.set(0.6);
        //     r_Up.set(0.6);

        // } else {
        //     l_Up.set(0);
        //     r_Up.set(0);
        //     SmartDashboard.putString("DOWN", "DOWN");
        // }

    }

    public void ArmMotion(double setpoint){
        double curpos = l_UpAbsoluteEncoder.getPosition(); 
        double error = setpoint - curpos;
        double kp = 0.01;
        double Accel_decel_val = (error)*0.3;
        //stage 1
        double Acceleration = curpos + Accel_decel_val;
        double Accelpow = 0.02;

        //stage 2
        double Stablepow = 0.4;

        //stage 3
        double Deceleration = error;
        double Decelpow = Stablepow * Deceleration;

        if(curpos <= Acceleration){
            l_Up.set(Accelpow);
            if(Accelpow<0.4){
            Accelpow +=0.01;
            }
        }
        else if(curpos >= Acceleration || curpos <= Deceleration){
                l_Up.set(Stablepow);
        }
        else if(curpos <= Deceleration ){
            l_Up.set(Decelpow);
        }
    }

    public void armToforauto(double sp, double power) {// Amp is 4.211 for left n 4.248 for right 243 deg
        double pos = (((Math.toDegrees(l_UpAbsoluteEncoder.getPosition())
                + Math.toDegrees(r_UpAbsoluteEncoder.getPosition()))) / 2.0);
        if (pos > sp + 10) {
            l_Up.set(-power);
            r_Up.set(-power);
        } else if (pos < sp - 10) {
            l_Up.set(power);
            r_Up.set(power);

        } else {
            l_Up.set(0);
            r_Up.set(0);
            SmartDashboard.putString("DOWN", "DOWN");
        }
    }

    public void display(double js) {
        SmartDashboard.putNumber("JS", js);
    }

    public void UpGetPosABS() {
        SmartDashboard.putNumber("l_Up", Math.toDegrees(l_UpAbsoluteEncoder.getPosition())); // 31.499735
        SmartDashboard.putNumber("r_Up", Math.toDegrees(r_UpAbsoluteEncoder.getPosition())); // -31.071171
        SmartDashboard.putNumber("l_up - r-up",
                Math.toDegrees(l_UpAbsoluteEncoder.getPosition()) - Math.toDegrees(r_UpAbsoluteEncoder.getPosition()));

    }

    public void upwithabsenc(double speed) {
        //double pos = (Math.toDegrees(l_UpAbsoluteEncoder.getPosition()));

        if (pos < 180) {
            l_Up.set(speed);
            // r_Up.set(speed);
        } else {
            l_Up.set(0);
            // r_Up.set(0);
        }
        pos = (Math.toDegrees(l_UpAbsoluteEncoder.getPosition()));
        SmartDashboard.putNumber("Position", pos);
    }
    public void UpwithReference() {
        pos = (Math.toDegrees(l_UpAbsoluteEncoder.getPosition()));
        double val = 10;
        pos = pos+val;
        if (pos < 180) {
            
            l_Up_pid.setReference(Math.toRadians(pos), ControlType.kPosition);
             SmartDashboard.putNumber("Position", pos);
            
            // r_Up.set(speed);
        }// else {
        //     l_Up.set(0);
        //     // r_Up.set(0);
        // }
        pos = (Math.toDegrees(l_UpAbsoluteEncoder.getPosition()));
       
    }
    public void DownwithReference() {
        pos = (Math.toDegrees(l_UpAbsoluteEncoder.getPosition()));
        double val = 10;
        pos = pos-val;
        if (pos > 67) {
            
            l_Up_pid.setReference(Math.toRadians(pos), ControlType.kPosition);
             SmartDashboard.putNumber("Position", pos); 
            // r_Up.set(speed);
        }// else {
        //     l_Up.set(0);
        //     // r_Up.set(0);
        // }
        pos = (Math.toDegrees(l_UpAbsoluteEncoder.getPosition()));
       
    }

    public void defaultMode(){
        l_Up_pid.setReference(Math.toRadians(pos), ControlType.kPosition);
        SmartDashboard.putNumber("Position", pos);
    }

    public void downwithabsenc(double speed) {
        pos = Math.toDegrees(l_UpAbsoluteEncoder.getPosition());
        if(pos>80){
            l_Up.set(-(speed));
        }
        else if (pos<80 && pos > 69) {
            // double pow = B1/(pos-76);

            l_Up.set(-(speed*(pos-68)*0.025));
        }
        else {
            l_Up.set(0);
        }
        // double pos = (Math.toDegrees(l_UpAbsoluteEncoder.getPosition()));
        // if(pos>)
        // if (pos > 69.5) {

        //     l_Up.set(-speed);
        //     // r_Up.set(-speed);
        // } else {
        //     l_Up.set(0);
        //     // r_Up.set(0);
        // }
        pos = (Math.toDegrees(l_UpAbsoluteEncoder.getPosition()));
        SmartDashboard.putNumber("Position", pos);
    }

    public void ll2SetArm() {
        targetX = tx.getDouble(0.0);
        targetY = ty.getDouble(0.0);
        targetA = ta.getDouble(0.0);
        targetV = (int) tv.getInteger(0);
        targetID = (int) tID.getInteger(0);

        if (targetV != 0) {

            if (targetID == 4 || targetID == 7 || targetID == 6) {
                actualDistance = (speakerTagHeight - camHeight) / Math.tan(Math.toRadians(camAngle + targetY));
            }
            // if (targetID == 1) {
            //     actualDistance = (ampTagHeight - camHeight) / Math.tan(Math.toRadians(camAngle + targetY));
            // }
            // actualDistance = actualDistance * 39.37;

            // r_Up_enc.setPosition(Math.abs(actualDistance));

            double pos = Math.toDegrees(l_UpAbsoluteEncoder.getPosition());

            // double error = angle - r_UpAbsoluteEncoder.getPosition();
            // double output = Math.abs(kP * error); // + kI * errorSum + kD * errorRate;

            // SmartDashboard.putNumber("9692 Power", output);

            lastDistance = actualDistance - 36;
            lastDistance = lastDistance * 0.268;
            angle = lastDistance + setAngle + 69;

            error = angle - pos;
            output = Math.abs(error * kP);

            if(output>=0.4)
                output = 0.4;

            SmartDashboard.putNumber("Absolute Encoder Values sss", pos);
            SmartDashboard.putNumber("9692 ActualDistance  is ssss: ", actualDistance);
            SmartDashboard.putNumber("9692 LastDistance  is ssss: ", lastDistance);
            SmartDashboard.putNumber("9692 Angle amde sss: ", angle);

            if (pos < angle + 1 && pos > angle - 1) {
                upwithabsenc(0.0);
            } else if ((pos > angle + 1) && (pos >= 69) && actualDistance >= 36 && actualDistance <= 150
                    && lastDistance >= 0) { // 35

                // r_Up_enc.setPosition(actualDistance);
                // Arm Down
                downwithabsenc(output);
            } else if ((pos < angle - 1) && (pos < 143.552) && actualDistance <= 150 && actualDistance >= 36
                    && lastDistance >= 0) { // 150

                // r_Up_enc.setPosition(actualDistance);
                // Arm Up
                upwithabsenc(output);
            } else {

                upwithabsenc(0.0);
            }
        
        } else {

            downwithabsenc(0.0);
        }

    }

    //     public void up(double increment) {
    //     double pos = (Math.toDegrees(l_UpAbsoluteEncoder.getPosition())+ Math.toDegrees(r_UpAbsoluteEncoder.getPosition())) / 2;

    //     if (pos < 180) {

    //         l_Up_pid.setReference(speed);
    //         // r_Up.set(speed);
    //     } else {
    //         l_Up.set(0);
    //         // r_Up.set(0);
    //     }
    // }
    
}