import React from 'react';
import styled from 'styled-components';

const LayoutContainer = styled.div`
  display: flex;
  flex-direction: column;
  height: 100vh;
`;

const MainContent = styled.main`
  flex: 1;
  overflow: hidden;
  display: flex;
`;

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
    // Log when component mounts
    React.useEffect(() => {
        console.log('[Layout] Component mounted');
        return () => {
            console.log('[Layout] Component will unmount');
        };
    }, []);
    // Log when children prop changes
    React.useEffect(() => {
        console.log('[Layout] Children updated:', {children});
    }, [children]);
    console.log('[Layout] Rendering component');

    return (
    <LayoutContainer>
      {/* Toolbar will go here */}
      {/* Menubar will go here */}
      <MainContent>
        {children}
      </MainContent>
    </LayoutContainer>
  );
};

export default Layout;